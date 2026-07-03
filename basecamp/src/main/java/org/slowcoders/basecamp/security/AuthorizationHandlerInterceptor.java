package org.slowcoders.basecamp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationHandlerInterceptor implements HandlerInterceptor {

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SessionInfo sessionInfo = SessionInfo.current();

        if (sessionInfo == null) {
            throw new RuntimeException("securityUserDetails must not be null.");
        }

        /** Todo
         * 1. Logout 여부 검사. (API User 제외)
         * 2. URL 과 userId 기반 API 접근 금지 여부 확인
         * 3. URL 기반 API 접근 권한 검사. (API User 제외)
         */
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) throws Exception {
        //
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav) throws Exception {
        //
    }

}
