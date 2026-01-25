package uz.javachi.devops_assignment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_SIZE = 10000; // 10KB max body size for logging
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "x-api-key", "x-auth-token"
    );
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator/health", "/actuator/prometheus", "/h2-console", "/swagger-ui", "/v3/api-docs"
    );

    private final ObjectMapper objectMapper;

    public LoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        MDC.put("requestId", requestId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("requestPath", request.getRequestURI());
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
            Map<String, Object> logData = new HashMap<>();
            logData.put("logType", "API_REQUEST");
            logData.put("requestId", requestId);
            logData.put("timestamp", Instant.now().toString());
            logData.put("httpMethod", request.getMethod());
            logData.put("apiUrl", request.getMethod() + " " + request.getRequestURI());
            logData.put("requestPath", request.getRequestURI());
            logData.put("queryString", Optional.ofNullable(request.getQueryString()).orElse(""));
            logData.put("responseCode", statusCode);
            logData.put("responseTime", processingTime);
            logData.put("clientIp", getClientIpAddress(request));
            logData.put("userAgent", Optional.ofNullable(request.getHeader("User-Agent")).orElse(""));

            // Request headers (sanitized)
            logData.put("requestHeaders", sanitizeHeaders(getRequestHeaders(request)));

            // Response headers
            logData.put("responseHeaders", getResponseHeaders(response));

            // Request body (limited size)
            String requestBody = getRequestBody(request);
            if (requestBody != null && !requestBody.isEmpty()) {
                logData.put("requestBody", sanitizeRequestBody(requestBody));
            }

            // Response body (limited size)
            String responseBody = getResponseBody(response);
            if (responseBody != null && !responseBody.isEmpty()) {
                logData.put("responseBody", truncateIfNeeded(responseBody));
            }

            // Log based on status code
            if (statusCode >= 400) {
                log.error("API Request Error: {}", objectMapper.writeValueAsString(logData));
            } else {
                log.info("API Request completed: {}", objectMapper.writeValueAsString(logData));
            }

        } catch (Exception e) {
            log.warn("Error logging request/response", e);
        }
    }

    private void logError(CachedBodyHttpServletRequest request, Exception e, long processingTime, String requestId) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("logType", "API_ERROR");
            logData.put("requestId", requestId);
            logData.put("timestamp", Instant.now().toString());
            logData.put("httpMethod", request.getMethod());
            logData.put("apiUrl", request.getMethod() + " " + request.getRequestURI());
            logData.put("requestPath", request.getRequestURI());
            logData.put("responseTime", processingTime);
            logData.put("clientIp", getClientIpAddress(request));
            logData.put("errorType", e.getClass().getSimpleName());
            logData.put("errorMessage", e.getMessage());
            logData.put("errorStackTrace", Arrays.toString(e.getStackTrace()));

            String requestBody = getRequestBody(request);
            if (requestBody != null && !requestBody.isEmpty()) {
                logData.put("requestBody", sanitizeRequestBody(requestBody));
            }

            log.error("API Request failed: {}", objectMapper.writeValueAsString(logData), e);

        } catch (Exception ex) {
            log.warn("Error logging error", ex);
        }
    }

    private Map<String, String> getRequestHeaders(CachedBodyHttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private Map<String, String> getResponseHeaders(CachedBodyHttpServletResponse response) {
        Map<String, String> headers = new HashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        for (String headerName : headerNames) {
            headers.put(headerName, response.getHeader(headerName));
        }
        return headers;
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String key = entry.getKey().toLowerCase();
                            if (SENSITIVE_HEADERS.contains(key)) {
                                return "***REDACTED***";
                            }
                            return entry.getValue();
                        }
                ));
    }

    private String getRequestBody(CachedBodyHttpServletRequest request) {
        try {
            String body = request.getCachedBodyAsString();
            return truncateIfNeeded(body);
        } catch (Exception e) {
            return null;
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

    private String sanitizeRequestBody(String body) {
        try {
            // Try to parse as JSON and sanitize sensitive fields
            if (body.trim().startsWith("{")) {
                Map<String, Object> json = objectMapper.readValue(body, Map.class);
                sanitizeJsonObject(json);
                return objectMapper.writeValueAsString(json);
            }
        } catch (Exception e) {
            // If not JSON or parsing fails, return as is (truncated)
        }
        return body;
    }

    private void sanitizeJsonObject(Map<String, Object> json) {
        Set<String> sensitiveFields = Set.of("password", "token", "secret", "apiKey", "api_key", "authorization");
        for (String key : json.keySet()) {
            if (sensitiveFields.contains(key.toLowerCase())) {
                json.put(key, "***REDACTED***");
            } else if (json.get(key) instanceof Map) {
                sanitizeJsonObject((Map<String, Object>) json.get(key));
            }
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
