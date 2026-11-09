package edu.itc.se.shop_jpa.web;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.itc.se.shop_jpa.service.InsufficientStockException;
import edu.itc.se.shop_jpa.service.ProductNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 400 Bad Request: Validation errors with field errors list
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request content."
        );
        problem.setTitle("Bad Request");

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    // 404 Not Found
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Not Found");
        return problem;
    }

    // 409 Conflict: Insufficient stock
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Conflict");
        return problem;
    }

    // 409 Conflict: Duplicate key / unique constraint violation
    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public ProblemDetail handleDuplicateKey(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A resource with the specified identifier or unique field already exists."
        );
        problem.setTitle("Conflict");
        return problem;
    }

    // 500 Internal Server Error: Generic fallback without leaking stack traces
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred."
        );
        problem.setTitle("Internal Server Error");
        return problem;
    }
}