package org.slowcoders.basecamp.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slowcoders.basecamp.security.SecurityUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slowcoders.basecamp.security.SessionInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TransactionLogger {
    private static final String HPMS_CONTROLLER = "execution(* org.slowcoders.basecamp..api.*Controller.*(..))";

    private static ThreadLocal<TransactionInfo> sessionInfoCache = new ThreadLocal<>();//.withInitial(() -> new HpmsSessionInfo());

    @Around(HPMS_CONTROLLER)
    public Object hookSession(ProceedingJoinPoint joinPoint) throws Throwable {
        initCurrentSession();
        Object result = joinPoint.proceed();
        return result;
    }

    public static TransactionInfo getCurrentSessionInfo() {
        TransactionInfo sessionInfo = sessionInfoCache.get();
        return sessionInfo;
    }

    private void initCurrentSession() {
        TransactionInfo session = null;

        SecurityUserDetails sud = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUserDetails) {
            sud = (SecurityUserDetails) authentication.getPrincipal();
        }
        if (sud != null) {
            String apiPath;
            try {
                apiPath = ServletUriComponentsBuilder.fromCurrentRequestUri().build().getPath();
            } catch (NullPointerException e) {
                log.error(e.getMessage());
                apiPath = "?Unknown?";
            } catch (Exception e) {
                apiPath = "?Unknown?";
            }

            SessionInfo sessionInfo = sud.getSessionInfo();
            session = new TransactionInfo(
                    sessionInfo.getLoginId(), sessionInfo.getUserLanguageCode(),
                    apiPath
            );
        }
        sessionInfoCache.set(session);

    }
}
