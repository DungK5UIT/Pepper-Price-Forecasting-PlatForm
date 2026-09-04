package com.giatieuviet.backend.api.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 9457 problem responses, so every endpoint reports
 * failures the same way instead of each controller inventing its own shape.
 *
 * Extends {@link ResponseEntityExceptionHandler} for the exceptions Spring MVC
 * raises itself — an unknown path, a wrong method, an unparseable parameter.
 * Those already carry the right status; without it the catch-all below would
 * swallow them and report every one of them as 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Raised by domain factory methods such as
     * {@link com.giatieuviet.backend.domain.Granularity#fromCode(String)} when a
     * request carries a value the domain does not accept. The message is written
     * for the caller, so it is safe to pass through.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    /**
     * Anything unanticipated — genuinely ours, since Spring's own web
     * exceptions are handled by the superclass. Logged with the stack trace
     * for diagnosis, reported to the caller without internal detail.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception while serving request", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error. Please try again later.");
        problem.setTitle("Internal server error");
        return problem;
    }
}
