package com.apda.consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailConsumerTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final EmailConsumer consumer = new EmailConsumer(mailSender, rabbitTemplate);

    @BeforeEach
    void setRecipient() throws Exception {
        Field recipient = EmailConsumer.class.getDeclaredField("recipient");
        recipient.setAccessible(true);
        recipient.set(consumer, "student@example.test");
    }

    @Test
    void movesLegacyEmailToDlqWhenSmtpSendFails() {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        consumer.terimaPesan("Tes pesan dari producer");

        verify(rabbitTemplate).convertAndSend("emailQueue.dlq", "Tes pesan dari producer");
    }

    @Test
    void propagatesFailureWhenLegacyDeadLetterCannotBePublished() {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        doThrow(new IllegalStateException("Rabbit unavailable"))
                .when(rabbitTemplate).convertAndSend("emailQueue.dlq", "Tes pesan dari producer");

        assertThrows(IllegalStateException.class, () -> consumer.terimaPesan("Tes pesan dari producer"));
    }
}
