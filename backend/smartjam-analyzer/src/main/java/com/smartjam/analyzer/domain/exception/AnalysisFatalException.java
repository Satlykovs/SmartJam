package com.smartjam.analyzer.domain.exception;

/**
 * Exception thrown when a non-recoverable error occurs during audio analysis. Indicates that the process cannot be
 * successfully retried (e.g., missing metadata in DB).
 */
public class AnalysisFatalException extends RuntimeException {
    public AnalysisFatalException(String message) {
        super(message);
    }
}
