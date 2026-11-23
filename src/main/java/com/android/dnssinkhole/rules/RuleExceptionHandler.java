package com.android.dnssinkhole.rules;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RuleController.class)
class RuleExceptionHandler {

    @ExceptionHandler(InvalidDomainException.class)
    ProblemDetail handleInvalidDomain(InvalidDomainException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RuleNotFoundException.class)
    ProblemDetail handleRuleNotFound(RuleNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
