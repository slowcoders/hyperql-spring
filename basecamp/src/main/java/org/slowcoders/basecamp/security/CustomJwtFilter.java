package org.slowcoders.basecamp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slowcoders.basecamp.security.config.AuthProperties;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomJwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        /**
         * path 검사 시 ${servlet.context-path} 값을 제외하고 비교하여야 한다.
         * publicUrl 등록시에도 contextPath 를 제외한 경로를 등록한다.
         */
        for (String pattern : authProperties.getPublicUrls()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    // 헤더에서 토큰 정보를 꺼내온다.
    public static String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = null;
        int httpErrCode = HttpServletResponse.SC_UNAUTHORIZED;
        Exception error = null;
        String errorMessage;

        try {
            String uri = request.getRequestURI();
            String jwt = resolveToken(request.getHeader(AUTHORIZATION_HEADER));
            authentication = tokenProvider.getAuthentication(jwt);
            SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        } catch (MalformedJwtException e) {
            error = e;
            errorMessage = "Malformed Token";
        } catch (ExpiredJwtException e) {
            error = e;
            errorMessage = "Expired Token";
        } catch (UnsupportedJwtException e) {
            error = e;
            errorMessage = "Unsupported Token";
        } catch (RuntimeException | ServletException e) {
            error = e;
            if (authentication == null) {
                errorMessage = "Invalid Token";
            } else {
                errorMessage = e.getMessage();
                httpErrCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
        }

        if (!response.isCommitted()) {
            log.error(errorMessage, error);
            response.setStatus(httpErrCode);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errorMessage));
        }
    }
}