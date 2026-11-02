package com.android.dnssinkhole.web;

import com.android.dnssinkhole.rules.InvalidDomainException;
import com.android.dnssinkhole.rules.RuleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidDomainException.class)
    public ProblemDetail handleInvalidDomain(InvalidDomainException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RuleNotFoundException.class)
    public ProblemDetail handleRuleNotFound(RuleNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
