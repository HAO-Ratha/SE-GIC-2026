package edu.itc.se.shop.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * One instance per HTTP request. It is injected into the singleton AuditAspect through a
 * scoped proxy: the proxy looks up the instance of the *current* request on every call.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {

    private final String requestId;
    private final String user;

    public RequestContext(HttpServletRequest request) {
        this.requestId = clean(request.getHeader("X-Request-Id"), UUID.randomUUID().toString().substring(0, 8));
        this.user = clean(request.getHeader("X-User"), "anonymous");
    }

    /** Keeps only safe characters so a header can never inject fake log lines. */
    private static String clean(String raw, String fallback) {
        if (raw == null) return fallback;
        String s = raw.replaceAll("[^A-Za-z0-9._@-]", "");
        if (s.length() > 36) s = s.substring(0, 36);
        return s.isEmpty() ? fallback : s;
    }

    public String requestId() { return requestId; }

    public String user() { return user; }
}