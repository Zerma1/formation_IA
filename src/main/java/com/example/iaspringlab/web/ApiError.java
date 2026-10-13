package com.example.iaspringlab.web;

import java.time.Instant;
import java.util.List;

/**
 * Standard error payload returned by {@link ApiExceptionHandler}.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
