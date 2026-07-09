package com.ssafy.fitbox.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI fitboxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FitBox API")
                        .description("FitBox 서버 API 문서")
                        .version("v1.0.0"));
    }
}