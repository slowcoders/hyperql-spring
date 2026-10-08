package org.slowcoders.basecamp.app.config;

import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import org.slowcoders.basecamp.security.CustomJwtFilter;
import org.slowcoders.basecamp.security.SecurityUtil;
import org.slowcoders.basecamp.security.TokenProvider;
import org.slowcoders.basecamp.security.config.AuthProperties;
import org.slowcoders.basecamp.security.config.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
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
public class SecurityConfig {

    private final AuthProperties authProperties;
    private final UserDetailsService userDetailsService;
    private final SecurityUtil securityUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return securityUtil.encrypt(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals(encode(rawPassword));
            }
        };
    }


    @Bean(name = "tokenProvider")
    public TokenProvider tokenProvider(AuthProperties authProperties) {
        JwtProperties jwt = authProperties.getJwtConfig();
        return new TokenProvider(
                Decoders.BASE64.decode(jwt.getSecret()),
                jwt.getAccessTokenValidityInSeconds(),
                jwt.getRefreshTokenValidityInSeconds(),
                userDetailsService
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity,
                                           CustomJwtFilter customJwtFilter) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // token을 사용하는 방식이기 때문에 csrf를 disable
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .exceptionHandling(exceptionConfig ->
//                        exceptionConfig.authenticationEntryPoint(jwtAuthenticationEntryPoint)
//                                .accessDeniedHandler(jwtAccessDeniedHandler)
//                )
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        //configuration.addAllowedOrigin("*");
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(false);
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
