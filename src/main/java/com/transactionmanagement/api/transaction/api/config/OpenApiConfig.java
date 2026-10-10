package com.transactionmanagement.api.transaction.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;

public class OpenApiConfig {

    @Bean
    public OpenAPI transactionManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Management API")
                        .version("1.0.0")
                        .description(
                                "REST API for managing transactions, " +
                                        "retrieving transaction IDs by type, " +
                                        "and calculating transitive transaction sums."
                        ));
    }
}
