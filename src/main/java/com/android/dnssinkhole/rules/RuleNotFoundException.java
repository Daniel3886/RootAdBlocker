package com.android.dnssinkhole.rules;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(Long id) {
        super("No rule with id " + id);
    }
}
