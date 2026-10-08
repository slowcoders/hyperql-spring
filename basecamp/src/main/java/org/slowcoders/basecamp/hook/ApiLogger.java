package org.slowcoders.basecamp.hook;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slowcoders.basecamp.app.ServiceException;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ApiLogger {
	
	@Around("execution(* org.slowcoders.basecamp.*.*Controller.*(..))")
	public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("Executing [{}] args: [{}]", joinPoint.getSignature(), joinPoint.getArgs());
        }
        try {
            return joinPoint.proceed();
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Exception occurred in [{}]", joinPoint.getSignature(), e);
            throw new ServiceException(e, e.getMessage());
        }
	}
}
