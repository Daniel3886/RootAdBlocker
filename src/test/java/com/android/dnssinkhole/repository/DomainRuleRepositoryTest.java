package com.android.dnssinkhole.repository;

import com.android.dnssinkhole.model.DomainRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DomainRuleRepositoryTest {

    @Autowired
    private DomainRuleRepository domainRuleRepository;


    @Test
    void findByDomainReturnsMatch() {
        domainRuleRepository.save(new DomainRule("ads.example.com", DomainRule.RuleType.BLOCK));

        Optional<DomainRule> rule = domainRuleRepository.findByDomain("ads.example.com");

        assertTrue(rule.isPresent());
        assertEquals(DomainRule.RuleType.BLOCK, rule.get().getType());
    }

    @Test
    void uniquenessConstraintWorks() {
        domainRuleRepository.saveAndFlush(new DomainRule("ads.example.com", DomainRule.RuleType.BLOCK));


        assertThrows(DataIntegrityViolationException.class, () ->
                domainRuleRepository.saveAndFlush(new DomainRule("ads.example.com", DomainRule.RuleType.BLOCK)));
    }


}