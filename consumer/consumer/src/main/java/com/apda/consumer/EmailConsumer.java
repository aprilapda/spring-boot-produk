
package com.apda.consumer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailConsumer {

    private final JavaMailSender mailSender;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.mail.recipient}")
    private String recipient;

    public EmailConsumer(JavaMailSender mailSender, RabbitTemplate rabbitTemplate) {
        this.mailSender = mailSender;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "emailQueue")
    public void terimaPesan(String pesan) {

        System.out.println("Pesan diterima dari RabbitMQ: " + pesan);

        try {
            SimpleMailMessage email = new SimpleMailMessage();

            email.setTo(recipient);
            email.setSubject("Informasi Pesanan");
            email.setText(
                "Halo pelanggan,\n\n" +
                "Kami telah menerima informasi berikut:\n\n" +
                pesan +
                "\n\nTerima kasih telah berbelanja."
            );

            mailSender.send(email);

            System.out.println("Email berhasil dikirim!");
        } catch (RuntimeException exception) {
            try {
                rabbitTemplate.convertAndSend("emailQueue.dlq", pesan);
                System.err.println("Gagal mengirim email; pesan dipindahkan ke emailQueue.dlq: "
                        + exception.getMessage());
            } catch (RuntimeException deadLetterFailure) {
                deadLetterFailure.addSuppressed(exception);
                throw new IllegalStateException("Gagal mengirim email dan memindahkan pesan ke DLQ",
                        deadLetterFailure);
            }
        }
    }
}
