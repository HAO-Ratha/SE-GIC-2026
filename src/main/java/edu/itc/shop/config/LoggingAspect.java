package edu.itc.shop.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // Logs before any method in the web package executes
    @Before("execution(* edu.itc.shop.web..*(..))")
    public void logBeforeWebMethods(JoinPoint joinPoint) {
        logger.info("Entering method: {}", joinPoint.getSignature().toShortString());
    }

    // Logs after the ProductService methods return
    @AfterReturning(pointcut = "execution(* edu.itc.shop.service.ProductService.*(..))", returning = "result")
    public void logAfterProductService(JoinPoint joinPoint, Object result) {
        logger.info("Method {} returned: {}", joinPoint.getSignature().toShortString(), result);
    }
}