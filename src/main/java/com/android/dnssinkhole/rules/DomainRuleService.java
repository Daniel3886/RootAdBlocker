package com.android.dnssinkhole.rules;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.blocklist.DomainNames;
import com.android.dnssinkhole.model.DomainRule;
import com.android.dnssinkhole.repository.DomainRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DomainRuleService {

    private final DomainRuleRepository domainRuleRepository;
    private final BlocklistService blocklistService;

    public DomainRuleService(DomainRuleRepository domainRuleRepository, BlocklistService blocklistService) {
        this.domainRuleRepository = domainRuleRepository;
        this.blocklistService = blocklistService;
    }

    public List<DomainRule> findAll() {
        return domainRuleRepository.findAll();
    }

    public DomainRule upsert(String rawDomain, DomainRule.RuleType type) {
        String domain = DomainNames.normalize(rawDomain);
        if (!DomainNames.isValid(domain)) {
            throw new InvalidDomainException(rawDomain);
        }

        DomainRule rule = domainRuleRepository.findByDomain(domain)
                .orElseGet(() -> new DomainRule(domain, type));
        rule.setType(type);

        DomainRule saved = domainRuleRepository.save(rule);
        blocklistService.applyRule(saved);
        return saved;
    }

    public void delete(Long id) {
        DomainRule rule = domainRuleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException(id));
        domainRuleRepository.delete(rule);
        blocklistService.removeRule(rule.getDomain());
    }
}
