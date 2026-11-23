package com.android.dnssinkhole.rules;

/**
 * Published when a BLOCK/ALLOW rule is created or its type changes.
 */
public record DomainRuleSavedEvent(String domain, RuleType type) {
}
