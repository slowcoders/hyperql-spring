package org.slowcoders.basecamp.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;



/**
 * <pre>
 * 스웨거 관련 설정 파일.
 * </pre>
 *
 * @author MoonHKLee
 * @version 3.0
 * @Modification <pre>
 *     since          author              description
 *  ===========    =============    ===========================
 *  2023-09-15     MoonHKLee            최초 생성
 * </pre>
 * Copyright (C) 2023 by LDCC., All right reserved.
 * @since 2023-09-15
 */
@Configuration
public class SwaggerConfig {

    private static final String HPMS_BASE_PACKAGE = "org.slowcoders.basecamp.hp";
    private static final String FWCM_BASE_PACKAGE = "org.slowcoders.basecamp.fw";
    private static final String GPPF_BASE_PACKAGE = "org.slowcoders.basecamp.gp";
    private static final String GMMS_BASE_PACKAGE = "org.slowcoders.basecamp.gm";

    @Bean
    public OpenAPI openAPI() {

        // SecuritySecheme명
        String jwtSchemeName = "Authorization";
        // API 요청헤더에 인증정보 포함
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        // SecuritySchemes 등록
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer")
                        .bearerFormat("JWT")); // 토큰 형식을 지정하는 임의의 문자(Optional)

        return new OpenAPI()
                .components(components)
                .security(Collections.singletonList(securityRequirement))
                .info(new Info()
                        .version("v1.0.0")
                        .title("PMS API SPEC")
                        .description("PMS API SPEC Description")
                );
    }

//    ApiGroup 추가 Sample
//    @Bean
//    public GroupedOpenApi hpbkApi() {
//        final String url = "hpbk";
//        return GroupedOpenApi.builder()
//                .group(url)
//                .pathsToMatch("/" + url + "/**")
//                .packagesToScan(HPMS_BASE_PACKAGE)
//                .build();
//    }

}
