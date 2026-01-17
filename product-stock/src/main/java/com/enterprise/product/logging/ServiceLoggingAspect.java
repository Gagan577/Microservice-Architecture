package com.enterprise.product.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Aspect for logging service layer method invocations.
 * Provides detailed logging for debugging and audit purposes.
 */
@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    private final ObjectMapper objectMapper;

    public ServiceLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Pointcut("execution(* com.enterprise.product.service.*.*(..))")
    public void serviceMethods() {
    }

    @Pointcut("execution(* com.enterprise.product.repository.*.*(..))")
    public void repositoryMethods() {
    }

    @Around("serviceMethods()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String correlationId = CorrelationIdManager.getCorrelationId();

        Map<String, Object> entryLog = new HashMap<>();
        entryLog.put("event", layer + "_METHOD_ENTRY");
        entryLog.put("correlationId", correlationId);
        entryLog.put("class", className);
        entryLog.put("method", methodName);

        // Log method arguments (be careful with sensitive data)
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String[] paramNames = signature.getParameterNames();
                Map<String, Object> params = new HashMap<>();
                for (int i = 0; i < args.length && i < paramNames.length; i++) {
                    if (args[i] != null && !isSensitive(paramNames[i])) {
                        params.put(paramNames[i], summarizeArg(args[i]));
                    }
                }
                entryLog.put("parameters", params);
            }
        } catch (Exception e) {
            entryLog.put("parameters", "Unable to serialize");
        }

        log.debug("Method entry: {}", objectMapper.writeValueAsString(entryLog));

        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> exitLog = new HashMap<>();
            exitLog.put("event", layer + "_METHOD_EXIT");
            exitLog.put("correlationId", correlationId);
            exitLog.put("class", className);
            exitLog.put("method", methodName);
            exitLog.put("durationMs", duration);

            if (exception != null) {
                exitLog.put("status", "ERROR");
                exitLog.put("exception", exception.getClass().getSimpleName());
                exitLog.put("message", exception.getMessage());
                log.error("Method exit with error: {}", objectMapper.writeValueAsString(exitLog));
            } else {
                exitLog.put("status", "SUCCESS");
                if (result != null) {
                    exitLog.put("resultType", result.getClass().getSimpleName());
                }
                log.debug("Method exit: {}", objectMapper.writeValueAsString(exitLog));
            }
        }
    }

    private boolean isSensitive(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("password") || lower.contains("secret") ||
                lower.contains("token") || lower.contains("credential");
    }

    private Object summarizeArg(Object arg) {
        if (arg == null) {
            return null;
        }
        String className = arg.getClass().getSimpleName();
        // For collections, just log the size
        if (arg instanceof java.util.Collection) {
            return className + "[size=" + ((java.util.Collection<?>) arg).size() + "]";
        }
        if (arg instanceof java.util.Map) {
            return className + "[size=" + ((java.util.Map<?, ?>) arg).size() + "]";
        }
        // For strings longer than 100 chars, truncate
        if (arg instanceof String && ((String) arg).length() > 100) {
            return ((String) arg).substring(0, 100) + "...";
        }
        return arg.toString();
    }
}
