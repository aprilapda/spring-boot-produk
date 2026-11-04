package com.apda.consumer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Listener typed untuk event ORDER_CREATED dari antrean `abl.email.order-created`.
 * Ini jadi jalur utama notifikasi email ketika Order dibuat dari service ORDER
 * (alur baru CQRS). Kegagalan SMTP dicoba ulang oleh listener container;
 * setelah batas percobaan habis, antrean typed mengirim pesan ke DLQ.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailOrderCreatedConsumer {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.recipient}")
    private String recipient;

    public EmailOrderCreatedConsumer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = OrderEventConstants.EMAIL_QUEUE)
    public void terimaEventOrderCreated(String payload) {
        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new AmqpRejectAndDontRequeueException("Payload event email rusak", exception);
        }

        String eventType = event.path("eventType").asText("");
        if (!OrderEventConstants.EVENT_ORDER_CREATED.equals(eventType)) {
            throw new AmqpRejectAndDontRequeueException("Tipe event email tidak didukung: " + eventType);
        }

        String eventId = event.path("eventId").asText("(tanpa id)");
        System.out.println("Event ORDER_CREATED diterima: " + eventId);

        JsonNode order = event.path("order");
        if (order.isMissingNode() || order.isNull()) {
            throw new AmqpRejectAndDontRequeueException(
                    "Event " + eventId + " tanpa snapshot order; tidak dapat dikirim email");
        }

        String deskripsi = buildDeskripsi(event, order);

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(recipient);
            email.setSubject("Informasi Pesanan");
            email.setText(
                    "Halo pelanggan,\n\n" +
                    "Kami telah menerima informasi berikut:\n\n" +
                    deskripsi +
                    "\n\nTerima kasih telah berbelanja."
            );

            mailSender.send(email);
            System.out.println("Email berhasil dikirim untuk event " + eventId + "!");
        } catch (RuntimeException exception) {
            System.err.println("Gagal mengirim email untuk event " + eventId
                    + "; listener akan mencoba ulang: " + exception.getMessage());
            throw exception;
        }
    }

    // Sama bentuk dengan notifikasi lama dari ORDER; snapshot event-time.
    private String buildDeskripsi(JsonNode event, JsonNode order) {
        JsonNode produk = event.path("produk");
        JsonNode pelanggan = event.path("pelanggan");

        String namaProduk;
        if (produk.isMissingNode() || produk.isNull()) {
            namaProduk = "Produk #" + order.path("produkId").asLong(0);
        } else {
            namaProduk = produk.path("nama").asText("Produk #" + order.path("produkId").asLong(0));
        }

        String namaPelanggan;
        if (pelanggan.isMissingNode() || pelanggan.isNull()) {
            namaPelanggan = "Pelanggan #" + order.path("pelangganId").asLong(0);
        } else {
            namaPelanggan = pelanggan.path("nama").asText("Pelanggan #" + order.path("pelangganId").asLong(0));
        }

        return "Order #" + order.path("id").asText("-")
                + "\nTanggal   : " + order.path("tglTrans").asText("-")
                + "\nProduk    : " + namaProduk
                + "\nPelanggan : " + namaPelanggan
                + "\nJumlah    : " + order.path("jumlah").asText("-")
                + "\nTotal     : Rp " + order.path("total").asText("-");
    }
}
