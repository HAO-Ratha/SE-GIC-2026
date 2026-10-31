package edu.itc.se.shop.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final RequestContext requestContext;
    private final Clock clock;

    public AuditAspect(RequestContext requestContext, Clock clock) {
        this.requestContext = requestContext;
        this.clock = clock;
    }

    @Around("@annotation(edu.itc.se.shop.audit.Audited)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        // The scoped proxy only works inside an HTTP request (not in a scheduler or a plain test).
        boolean inRequest = RequestContextHolder.getRequestAttributes() != null;
        String requestId = inRequest ? requestContext.requestId() : "-";
        String who = inRequest ? requestContext.user() : "system";

        String what = pjp.getSignature().getDeclaringType().getSimpleName() + "." + pjp.getSignature().getName();
        Instant when = Instant.now(clock);
        long start = System.nanoTime();

        MDC.put("requestId", requestId);
        try {
            Object result = pjp.proceed();
            log.info("AUDIT {} user={} args={} result={} at={} ({} ms)",
                    what, who, Arrays.toString(pjp.getArgs()), result, when, elapsedMs(start));
            return result;
        } catch (Throwable ex) {
            log.warn("AUDIT {} user={} args={} FAILED {}: {} at={} ({} ms)",
                    what, who, Arrays.toString(pjp.getArgs()), ex.getClass().getSimpleName(), ex.getMessage(),
                    when, elapsedMs(start));
            throw ex;
        } finally {
            MDC.remove("requestId");
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}