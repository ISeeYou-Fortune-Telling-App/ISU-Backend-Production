package com.iseeyou.foretunetelling.configs;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfig {

    @Bean
    public TopicExchange reportExchange() {
        return new TopicExchange("report.exchange", true, false);
    }

    @Bean
    public Queue userChangeQueue() {
        return QueueBuilder.durable("user.change.queue")
                .withArgument("x-dead-letter-exchange", "report.dlx")
                .build();
    }

    @Bean
    public Queue userActionQueue() {
        return QueueBuilder.durable("user.action.queue")
                .withArgument("x-dead-letter-exchange", "report.dlx")
                .build();
    }

    @Bean
    public Binding userChangeBinding(Queue userChangeQueue, TopicExchange reportExchange) {
        return BindingBuilder.bind(userChangeQueue)
                .to(reportExchange)
                .with("user.change");
    }

    @Bean
    public Binding userActionBinding(Queue userActionQueue, TopicExchange reportExchange) {
        return BindingBuilder.bind(userActionQueue)
                .to(reportExchange)
                .with("user.action");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}