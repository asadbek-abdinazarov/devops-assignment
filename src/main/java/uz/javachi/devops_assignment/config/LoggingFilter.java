package uz.javachi.devops_assignment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_SIZE = 10000; // 10KB max body size for logging
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator/health", "/actuator/prometheus", "/h2-console", "/swagger-ui", "/v3/api-docs"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip logging for excluded paths
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // Wrap request and response
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        CachedBodyHttpServletResponse wrappedResponse = new CachedBodyHttpServletResponse(response);

        try {
            // Set MDC context
            setMDCContext(wrappedRequest, requestId);

            // Process request
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
            // Response body is already written to the underlying response by the wrapper
            // We just need to flush if using writer
            if (wrappedResponse.getCachedBody().length > 0) {
                // Body already written, just ensure it's flushed
            }

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;

            // Log request/response
            logRequestResponse(wrappedRequest, wrappedResponse, response.getStatus(), processingTime, requestId);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logError(wrappedRequest, e, processingTime, requestId);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private void setMDCContext(CachedBodyHttpServletRequest request, String requestId) {
        // Only set fields that are needed in the final log output
        MDC.put("queryString", Optional.ofNullable(request.getQueryString()).orElse(""));
        MDC.put("clientIp", getClientIpAddress(request));
        MDC.put("userAgent", Optional.ofNullable(request.getHeader("User-Agent")).orElse(""));
    }

    private void logRequestResponse(CachedBodyHttpServletRequest request, 
                                   CachedBodyHttpServletResponse response, 
                                   int statusCode, 
                                   long processingTime,
                                   String requestId) {
        try {
            // Set MDC for structured logging (will be included in JSON by logback encoder)
            MDC.put("logType", "API_REQUEST");
            MDC.put("apiUrl", request.getMethod() + " " + request.getRequestURI());
            MDC.put("responseCode", String.valueOf(statusCode));
            MDC.put("responseTime", String.valueOf(processingTime));

            // Response body (limited size) - only this is needed
            String responseBody = getResponseBody(response);
            if (responseBody != null && !responseBody.isEmpty()) {
                MDC.put("responseBody", truncateIfNeeded(responseBody));
            }

            // Log based on status code
            if (statusCode >= 400) {
                log.error("API Request Error");
            } else {
                log.info("API Request completed");
            }

            // Clean up MDC (responseBody will be cleared in finally block)
            MDC.remove("logType");
            MDC.remove("apiUrl");
            MDC.remove("responseCode");
            MDC.remove("responseTime");
            MDC.remove("responseBody");

        } catch (Exception e) {
            log.warn("Error logging request/response", e);
        }
    }

    private void logError(CachedBodyHttpServletRequest request, Exception e, long processingTime, String requestId) {
        try {
            // Set MDC for structured logging
            MDC.put("logType", "API_ERROR");
            MDC.put("apiUrl", request.getMethod() + " " + request.getRequestURI());
            MDC.put("responseTime", String.valueOf(processingTime));
            MDC.put("errorType", e.getClass().getSimpleName());
            MDC.put("errorMessage", e.getMessage());

            log.error("API Request failed", e);

            // Clean up MDC
            MDC.remove("logType");
            MDC.remove("apiUrl");
            MDC.remove("responseTime");
            MDC.remove("errorType");
            MDC.remove("errorMessage");

        } catch (Exception ex) {
            log.warn("Error logging error", ex);
        }
    }

    private String getResponseBody(CachedBodyHttpServletResponse response) {
        try {
            String body = response.getCachedBodyAsString();
            return truncateIfNeeded(body);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncateIfNeeded(String body) {
        if (body == null) {
            return null;
        }
        if (body.length() > MAX_BODY_SIZE) {
            return body.substring(0, MAX_BODY_SIZE) + "... [TRUNCATED]";
        }
        return body;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs (X-Forwarded-For can contain multiple IPs)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
