package edu.itc.shop.web;

import edu.itc.shop.service.ProductNotFoundException;
import edu.itc.shop.service.InsufficientStockException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    // 400 - Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed"
        );

        problem.setTitle("Bad Request");

        List<Map<String, String>> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> Map.of(
                                "field", error.getField(),
                                "message", error.getDefaultMessage()
                        ))
                        .toList();

        problem.setProperty("errors", errors);

        return problem;
    }

    // 404 - Product not found
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(
            ProductNotFoundException ex
    ) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    // 409 - Insufficient stock
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(
            InsufficientStockException ex
    ) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    // 409 - Duplicate key
    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicateKey(
            DuplicateKeyException ex
    ) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A duplicate record already exists"
        );
    }

    // 500 - Unexpected error
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred"
        );
    }
}