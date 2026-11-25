package com.android.dnssinkhole.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainRuleTest {

    @Test
    void changeTypeToSwitchesBetweenBlockAndAllow() {
        DomainRule rule = new DomainRule("ads.example.com", RuleType.BLOCK);

        rule.changeTypeTo(RuleType.ALLOW);

        assertEquals(RuleType.ALLOW, rule.getType());
    }

    @Test
    void rejectsMissingType() {
        assertThrows(NullPointerException.class, () -> new DomainRule("ads.example.com", null));

        DomainRule rule = new DomainRule("ads.example.com", RuleType.BLOCK);
        assertThrows(NullPointerException.class, () -> rule.changeTypeTo(null));
        assertEquals(RuleType.BLOCK, rule.getType());
    }
}
