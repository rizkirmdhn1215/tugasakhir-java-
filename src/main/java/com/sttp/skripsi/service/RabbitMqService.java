package com.sttp.skripsi.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RabbitMqService {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendToQueue(String queueName, Object message) {
        logger.info("About to send message to RabbitMQ queue '{}': {}", queueName, message);
        try {
            rabbitTemplate.convertAndSend(queueName, message);
            logger.info("Message sent to RabbitMQ queue '{}'", queueName);
        } catch (Exception e) {
            logger.error("Failed to send message to RabbitMQ queue '{}': {}", queueName, e.getMessage(), e);
        }
    }
}