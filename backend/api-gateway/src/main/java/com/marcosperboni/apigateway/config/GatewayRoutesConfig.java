package com.marcosperboni.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder,
            @Value("${services.customer.url:http://localhost:8081}") String customerUrl,
            @Value("${services.account.url:http://localhost:8082}") String accountUrl,
            @Value("${services.transaction.url:http://localhost:8083}") String transactionUrl,
            @Value("${services.payment.url:http://localhost:8084}") String paymentUrl,
            @Value("${services.notification.url:http://localhost:8085}") String notificationUrl) {
        return builder.routes()
                .route("customer-service", r -> r.path("/api/customers/**").uri(customerUrl))
                .route("account-service", r -> r.path("/api/accounts/**").uri(accountUrl))
                .route("transaction-service", r -> r.path("/api/transactions/**").uri(transactionUrl))
                .route("payment-service", r -> r.path("/api/payments/**").uri(paymentUrl))
                .route("notification-service", r -> r.path("/api/notifications/**").uri(notificationUrl))
                .build();
    }
}
