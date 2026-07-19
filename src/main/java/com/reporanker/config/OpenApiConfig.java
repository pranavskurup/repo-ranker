package com.reporanker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the Repo Ranker API.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI specification for the application.
     *
     * @return the OpenAPI configuration
     */
    @Bean
    public OpenAPI repoRankerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Repo Ranker API")
                        .description("API for scoring and ranking GitHub repositories based on popularity metrics")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Pranav")
                                .url("https://github.com/pranavskurup"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
