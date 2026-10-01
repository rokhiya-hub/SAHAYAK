package com.drrcp.reporting.aspect;

import com.drrcp.reporting.service.AuditLogWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i)(\\\"(?:token|password|secret|aadharNumber|aadhaarNumber|phone)\\\"\\s*:\\s*\\\")[^\\\"]*\\\"");

    private final AuditLogWriter auditLogWriter;
    private final ObjectMapper objectMapper;

    @Around("@within(org.springframework.stereotype.Service) && ("
            + "execution(public * allocate*(..)) || execution(public * checkin*(..)) || "
            + "execution(public * checkout*(..)) || execution(public * assign*(..)) || "
            + "execution(public * register*(..)) || execution(public * fulfill*(..)))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.nanoTime();
        String action = joinPoint.getSignature().toShortString();
        String actor = currentActor();
        boolean success = false;
        String outcome = "unknown";
        try {
            Object result = joinPoint.proceed();
            success = true;
            outcome = "success";
            return result;
        } catch (Throwable error) {
            outcome = "failure: " + error.getClass().getSimpleName();
            throw error;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            String details = "args=" + safeArguments(joinPoint.getArgs())
                    + "; durationMs=" + durationMs + "; outcome=" + outcome;
            auditLogWriter.write(action, actor, success, details);
        }
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? "anonymous" : authentication.getName();
    }

    private String safeArguments(Object[] args) {
        try {
            String json = objectMapper.writeValueAsString(Arrays.asList(args));
            return SENSITIVE_VALUE.matcher(json).replaceAll("$1[REDACTED]\"");
        } catch (JsonProcessingException ex) {
            return Arrays.stream(args).map(arg -> arg == null ? "null" : arg.getClass().getSimpleName()).toList().toString();
        }
    }
}