package com.iseeyou.fortunetelling.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // ========== Notification Exchange ==========
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange("notification.exchange", true, false);
    }

    // ========== Report Exchange ==========
    @Bean
    public TopicExchange reportExchange() {
        return new TopicExchange("report.exchange", true, false);
    }

    // ========== User Exchange (cho Push Notification Microservice) ==========
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange("user.exchange", true, false);
    }

    // ========== User Login Queue ==========
    @Bean
    public Queue userLoginQueue() {
        return QueueBuilder.durable("user.login.queue")
                .withArgument("x-dead-letter-exchange", "user.dlx")
                .build();
    }

    @Bean
    public Binding userLoginBinding(Queue userLoginQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userLoginQueue)
                .to(userExchange)
                .with("user.login");
    }

    // ========== User Logout Queue ==========
    @Bean
    public Queue userLogoutQueue() {
        return QueueBuilder.durable("user.logout.queue")
                .withArgument("x-dead-letter-exchange", "user.dlx")
                .build();
    }

    @Bean
    public Binding userLogoutBinding(Queue userLogoutQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userLogoutQueue)
                .to(userExchange)
                .with("user.logout");
    }

    // ========== User Delete Queue ==========
    @Bean
    public Queue userDeleteQueue() {
        return QueueBuilder.durable("user.delete.queue")
                .withArgument("x-dead-letter-exchange", "user.dlx")
                .build();
    }

    @Bean
    public Binding userDeleteBinding(Queue userDeleteQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeleteQueue)
                .to(userExchange)
                .with("user.delete");
    }

    // ========== Seer Rating Queue ==========
    @Bean
    public Queue seerNewRatingQueue() {
        return QueueBuilder.durable("seer.rating.queue")
                .withArgument("x-dead-letter-exchange", "seer_rating.dlx")
                .build();
    }

    @Bean
    public Binding seerNewRatingBinding(Queue seerNewRatingQueue, TopicExchange reportExchange) {
        return BindingBuilder.bind(seerNewRatingQueue)
                .to(reportExchange)
                .with("seer.rating");
    }

    // ========== Message Converter ==========
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}