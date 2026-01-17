package com.enterprise.shop.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP-based logging aspect for service layer operations.
 * Provides detailed logging for business logic execution.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ServiceLoggingAspect {

    private final CorrelationIdManager correlationIdManager;

    /**
     * Pointcut for all service layer methods.
     */
    @Pointcut("within(com.enterprise.shop.service..*)")
    public void serviceLayer() {}

    /**
     * Pointcut for all repository layer methods.
     */
    @Pointcut("within(com.enterprise.shop.repository..*)")
    public void repositoryLayer() {}

    /**
     * Pointcut for all controller layer methods.
     */
    @Pointcut("within(com.enterprise.shop.controller..*)")
    public void controllerLayer() {}

    /**
     * Log service method execution.
     */
    @Around("serviceLayer()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    /**
     * Log repository method execution.
     */
    @Around("repositoryLayer()")
    public Object logRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "REPOSITORY");
    }

    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String traceId = correlationIdManager.getTraceId();

        long startTime = System.currentTimeMillis();

        try {
            // Log method entry
            log.debug("[{}] [{}] Entering {}.{}() with arguments: {}",
                    layer,
                    traceId,
                    className,
                    methodName,
                    formatArguments(joinPoint.getArgs()));

            // Execute method
            Object result = joinPoint.proceed();

            // Log method exit
            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("[{}] [{}] Exiting {}.{}() - Execution time: {}ms",
                    layer,
                    traceId,
                    className,
                    methodName,
                    executionTime);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Set error stack in MDC for structured logging
            MDC.put(CorrelationIdManager.ERROR_STACK_KEY, getStackTraceAsString(e));

            log.error("[{}] [{}] Exception in {}.{}() - Execution time: {}ms - Error: {}",
                    layer,
                    traceId,
                    className,
                    methodName,
                    executionTime,
                    e.getMessage());

            throw e;
        }
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(this::formatArgument)
                .toList()
                .toString();
    }

    private String formatArgument(Object arg) {
        if (arg == null) {
            return "null";
        }
        String className = arg.getClass().getSimpleName();
        String value = arg.toString();
        // Truncate long values
        if (value.length() > 200) {
            value = value.substring(0, 200) + "...";
        }
        return className + "=" + value;
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage());
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (element.getClassName().startsWith("com.enterprise")) {
                sb.append("\n\tat ").append(element);
            }
        }
        return sb.toString();
    }
}
