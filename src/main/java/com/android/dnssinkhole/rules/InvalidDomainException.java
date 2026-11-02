package com.android.dnssinkhole.rules;

public class InvalidDomainException extends RuntimeException {

    public InvalidDomainException(String domain) {
        super("Not a valid domain name: " + domain);
    }
}
