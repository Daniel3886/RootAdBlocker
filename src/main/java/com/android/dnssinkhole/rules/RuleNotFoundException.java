package com.android.dnssinkhole.rules;

class RuleNotFoundException extends RuntimeException {

    RuleNotFoundException(Long id) {
        super("No rule with id " + id);
    }
}
