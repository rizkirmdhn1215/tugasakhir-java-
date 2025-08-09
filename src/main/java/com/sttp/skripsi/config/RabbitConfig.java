package com.sttp.skripsi.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public Queue processQueue() {
        return new Queue("sheet.process", false);
    }

    @Bean
    public Queue resultQueue() {
        return new Queue("sheet.process.result", false);
    }
}