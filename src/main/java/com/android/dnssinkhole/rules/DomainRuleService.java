package com.android.dnssinkhole.rules;

import com.android.dnssinkhole.common.DomainNames;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DomainRuleService {

    private final DomainRuleRepository domainRuleRepository;
    private final ApplicationEventPublisher events;

    DomainRuleService(DomainRuleRepository domainRuleRepository, ApplicationEventPublisher events) {
        this.domainRuleRepository = domainRuleRepository;
        this.events = events;
    }

    /**
     * Snapshot of every rule as domain -> type, for features that keep their own cache.
     */
    @Transactional(readOnly = true)
    public Map<String, RuleType> currentRules() {
        return domainRuleRepository.findAll().stream()
                .collect(Collectors.toMap(DomainRule::getDomain, DomainRule::getType));
    }

    @Transactional(readOnly = true)
    List<DomainRule> findAll() {
        return domainRuleRepository.findAll();
    }

    @Transactional
    DomainRule upsert(String rawDomain, RuleType type) {
        String domain = DomainNames.normalize(rawDomain);
        if (!DomainNames.isValid(domain)) {
            throw new InvalidDomainException(rawDomain);
        }

        DomainRule rule = domainRuleRepository.findByDomain(domain)
                .orElseGet(() -> new DomainRule(domain, type));
        rule.setType(type);

        DomainRule saved = domainRuleRepository.save(rule);
        events.publishEvent(new DomainRuleSavedEvent(saved.getDomain(), saved.getType()));
        return saved;
    }

    @Transactional
    void delete(Long id) {
        DomainRule rule = domainRuleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException(id));
        domainRuleRepository.delete(rule);
        events.publishEvent(new DomainRuleDeletedEvent(rule.getDomain()));
    }
}
