package com.example.hrapp.observability.example;

import com.example.hrapp.logging.CorrelationIdFilter;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SentryTracingConfigExample {

    @Bean
    public OncePerRequestFilter sentryTracingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
            ) throws ServletException, IOException {
                // Example only: register this filter after CorrelationIdFilter so the header is available.
                String correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
                }

                ITransaction transaction = Sentry.startTransaction(
                    request.getMethod() + " " + request.getRequestURI(),
                    "http.server"
                );

                String finalCorrelationId = correlationId;
                Sentry.configureScope(scope -> {
                    if (finalCorrelationId != null && !finalCorrelationId.isBlank()) {
                        scope.setTag("correlation_id", finalCorrelationId);
                    }
                    scope.setTag("http.method", request.getMethod());
                    scope.setTag("http.path", request.getRequestURI());
                    scope.setTransaction(transaction.getName());
                });

                try {
                    filterChain.doFilter(request, response);
                    transaction.setStatus(mapHttpStatus(response.getStatus()));
                } catch (Throwable ex) {
                    transaction.setThrowable(ex);
                    transaction.setStatus(SpanStatus.INTERNAL_ERROR);
                    Sentry.captureException(ex);
                    throw ex;
                } finally {
                    transaction.finish();
                }
            }

            private SpanStatus mapHttpStatus(int statusCode) {
                if (statusCode >= 500) {
                    return SpanStatus.INTERNAL_ERROR;
                }
                if (statusCode >= 400) {
                    return SpanStatus.INVALID_ARGUMENT;
                }
                return SpanStatus.OK;
            }
        };
    }
}
