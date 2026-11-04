
package com.apda.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
public class EmailProducer {

    private final RabbitTemplate rabbitTemplate;

    public EmailProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public String sendEmail(@RequestBody String pesan) {
        rabbitTemplate.convertAndSend("emailQueue", pesan);

        return "Pesan berhasil dikirim ke RabbitMQ: " + pesan;
    }
}