package com.android.dnssinkhole.rules;

class InvalidDomainException extends RuntimeException {

    InvalidDomainException(String domain) {
        super("Not a valid domain name: " + domain);
    }
}
