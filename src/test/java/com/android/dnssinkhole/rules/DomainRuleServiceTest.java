package com.android.dnssinkhole.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainRuleServiceTest {

    @Mock
    private DomainRuleRepository domainRuleRepository;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private DomainRuleService domainRuleService;

    @Test
    void upsertNormalizesDomainAndUpdatesCache() {
        when(domainRuleRepository.findByDomain("ads.example.com")).thenReturn(Optional.empty());
        when(domainRuleRepository.save(any(DomainRule.class))).thenAnswer(inv -> inv.getArgument(0));

        DomainRule saved = domainRuleService.upsert("  Ads.Example.COM. ", RuleType.BLOCK);

        assertEquals("ads.example.com", saved.getDomain());
        assertEquals(RuleType.BLOCK, saved.getType());
        verify(events).publishEvent(new DomainRuleSavedEvent("ads.example.com", RuleType.BLOCK));
    }

    @Test
    void upsertChangesTypeOfExistingRule() {
        DomainRule existing = new DomainRule("ads.example.com", RuleType.BLOCK);
        when(domainRuleRepository.findByDomain("ads.example.com")).thenReturn(Optional.of(existing));
        when(domainRuleRepository.save(existing)).thenReturn(existing);

        DomainRule saved = domainRuleService.upsert("ads.example.com", RuleType.ALLOW);

        assertSame(existing, saved);
        assertEquals(RuleType.ALLOW, saved.getType());
        verify(events).publishEvent(new DomainRuleSavedEvent("ads.example.com", RuleType.ALLOW));
    }

    @Test
    void upsertRejectsInvalidDomain() {
        assertThrows(InvalidDomainException.class,
                () -> domainRuleService.upsert("not a domain", RuleType.BLOCK));
        verify(domainRuleRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void deleteRemovesRuleFromCache() {
        DomainRule rule = new DomainRule("ads.example.com", RuleType.BLOCK);
        when(domainRuleRepository.findById(1L)).thenReturn(Optional.of(rule));

        domainRuleService.delete(1L);

        verify(domainRuleRepository).delete(rule);
        verify(events).publishEvent(new DomainRuleDeletedEvent("ads.example.com"));
    }

    @Test
    void deleteUnknownIdThrowsNotFound() {
        when(domainRuleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuleNotFoundException.class, () -> domainRuleService.delete(99L));
        verifyNoInteractions(events);
    }
}
