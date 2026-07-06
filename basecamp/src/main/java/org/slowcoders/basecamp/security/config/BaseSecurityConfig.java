package org.slowcoders.basecamp.security.config;

import lombok.RequiredArgsConstructor;
import org.slowcoders.basecamp.security.CustomJwtFilter;
import org.slowcoders.basecamp.security.SecurityUtil;
import org.slowcoders.basecamp.security.TokenProvider;
import org.slowcoders.basecamp.security.AbstractUserDetailsService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties({AuthProperties.class})
@RequiredArgsConstructor
public class BaseSecurityConfig {

    private final AuthProperties authProperties;
    private final UserDetailsService userDetailsService;

    @Bean(name = "tokenProvider")
    public TokenProvider tokenProvider(AuthProperties authProperties) {
        return new TokenProvider(
//                Decoders.BASE64.decode(authProperties.getSecret())
                        authProperties.getSecret().getBytes(),
                authProperties.getAccessTokenPeriodInSec(),
                authProperties.getRefreshTokenPeriodInSec(),
                userDetailsService
        );
    }

    @Bean
    public AuthenticationProvider authenticationProvider(AbstractUserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserCache(userDetailsService);
        return provider;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity,
                                           CustomJwtFilter customJwtFilter,
                                           CorsConfiguration corsConfiguration) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // token을 사용 시 csrf를 disable
                /**
                 * Spring Security 사용 시 MVC 설정보다 Security Filter가 먼저 작동하므로,
                 * Security 설정 클래스 내에서 CORS 설정을 활성화해 주어야만 한다.
                 */
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsConfiguration)))
                .headers(headerConfig ->
                        headerConfig.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                            // 설정 파일의 exclude URLs는 permitAll
                            authProperties.getPublicUrls().forEach(url ->
                                auth.requestMatchers(url).permitAll()
                            );
                            // 나머지는 인증 필요
                            auth.anyRequest().authenticated();
                })
                .addFilterBefore(customJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    @ConfigurationProperties(prefix = "basecamp.cors")
    public CorsConfiguration corsProperties() {
        return new CorsConfiguration();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsConfiguration configuration) {
//        CorsConfiguration configuration = new CorsConfiguration();
//        //configuration.addAllowedOrigin("*");
//        configuration.addAllowedOriginPattern("*");
//        configuration.addAllowedHeader("*");
//        configuration.addAllowedMethod("*");
//        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

//    @Bean
//    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, SecurityUtil securityUtil, SessionUtil sessionUtil) {
//        return new CustomAuthenticationProvider(userDetailsService, securityUtil, sessionUtil);
//    }
}
