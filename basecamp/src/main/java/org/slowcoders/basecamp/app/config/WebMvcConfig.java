package org.slowcoders.basecamp.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slowcoders.basecamp.security.AuthorizationHandlerInterceptor;
import org.slowcoders.basecamp.security.config.AuthProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthorizationHandlerInterceptor authorizationHandlerInterceptor;
    private final AuthProperties authProperties;

//    @Value("${spring.webservice.intro}")
//    private String introPage;

//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        // 루트 (/) 로 접근 시 introPage로 이동하는 매핑 추가
//        registry.addRedirectViewController("/", introPage);
//    }

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry
//                .addResourceHandler("/**")
//                .addResourceLocations("classpath:/init/");
//    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /**
         * # Interceptor 처리 과정
         *   Filter 이후 처리 (Authentication 이후, 권한(Authorization) 제어를 위해 사용)
         *
         * <클라이언트 요청>
         *     ↓
         * ① Filter (doFilter) - 예: CustomJwtFilter
         *     ↓
         * DispatcherServlet
         *     ↓
         * ② Interceptor (preHandle) - 예: AuthorizationHandlerInterceptor, EodHandlerInterceptor
         *     ↓
         * Controller (핸들러 메서드 실행)
         *     ↓
         * ③ Interceptor (postHandle)
         *     ↓
         * View Rendering
         *     ↓
         * ④ Interceptor (afterCompletion)
         *     ↓
         * ⑤ Filter (doFilter 계속)
         *     ↓
         * <응답>
         */

        registry.addInterceptor(authorizationHandlerInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(authProperties.getPublicUrls());

    }
}