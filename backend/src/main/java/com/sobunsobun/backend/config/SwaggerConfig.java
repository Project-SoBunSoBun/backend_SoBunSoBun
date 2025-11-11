package com.sobunsobun.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("소분소분 API")
                        .version("v1.0")
                        .description("공동구매 플랫폼 소분소분 API 문서\n\n" +
                                "JWT 인증이 필요한 API의 경우 우측 상단의 Authorize 버튼을 클릭하여 토큰을 입력하세요."))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Access Token을 입력해주세요. (Bearer 키워드 제외)")
                        )
                )
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .pathsToExclude("/error") // 에러 엔드포인트 제외
                .build();
    }
}
