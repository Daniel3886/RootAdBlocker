package com.android.dnssinkhole.rules;

/**
 * Published when a BLOCK/ALLOW rule is removed.
 */
public record DomainRuleDeletedEvent(String domain) {
}
