package com.apda.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;

class EmailOrderCreatedConsumerTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailOrderCreatedConsumer consumer = new EmailOrderCreatedConsumer(mailSender);

    @BeforeEach
    void setRecipient() throws Exception {
        Field recipient = EmailOrderCreatedConsumer.class.getDeclaredField("recipient");
        recipient.setAccessible(true);
        recipient.set(consumer, "student@example.test");
    }

    @Test
    void sendsEmailFromCreatedOrderSnapshot() {
        consumer.terimaEventOrderCreated(createdEvent());

        ArgumentCaptor<SimpleMailMessage> emailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailCaptor.capture());
        SimpleMailMessage sentEmail = emailCaptor.getValue();
        assertEquals("student@example.test", sentEmail.getTo()[0]);
        assertEquals("Informasi Pesanan", sentEmail.getSubject());
        org.junit.jupiter.api.Assertions.assertTrue(sentEmail.getText().contains("Laptop"));
        org.junit.jupiter.api.Assertions.assertTrue(sentEmail.getText().contains("Amel"));
        org.junit.jupiter.api.Assertions.assertTrue(sentEmail.getText().contains("Order #7"));
    }

    @Test
    void propagatesSmtpFailureSoListenerRetryAndDlqCanRun() {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class, () -> consumer.terimaEventOrderCreated(createdEvent()));
    }

    @Test
    void rejectsMalformedEventForDeadLetterQueue() {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer.terimaEventOrderCreated("{broken-json"));
    }

    private String createdEvent() {
        return """
                {
                  "eventId":"event-7",
                  "eventType":"ORDER_CREATED",
                  "order":{"id":7,"produkId":1,"pelangganId":2,"tglTrans":"2026-10-30","jumlah":2,"total":24000000},
                  "produk":{"id":1,"nama":"Laptop"},
                  "pelanggan":{"id":2,"nama":"Amel"}
                }
                """;
    }
}
