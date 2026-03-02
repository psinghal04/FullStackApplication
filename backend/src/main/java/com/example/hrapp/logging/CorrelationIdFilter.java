package com.example.hrapp.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns and propagates a request correlation identifier.
 *
 * <p>The filter ensures every request has an {@code X-Correlation-Id}, mirrors it back in the
 * response, and exposes it through MDC so log lines across layers can be stitched together.</p>
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String incomingCorrelationId = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId = (incomingCorrelationId == null || incomingCorrelationId.isBlank())
            ? UUID.randomUUID().toString()
            : incomingCorrelationId;

        long startTime = System.currentTimeMillis();
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            log.info(
                "request_completed method={} path={} status={} durationMs={} correlationId={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                correlationId
            );
            MDC.remove(MDC_KEY);
        }
    }
}
