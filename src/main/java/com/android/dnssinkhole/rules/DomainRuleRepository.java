package com.android.dnssinkhole.rules;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface DomainRuleRepository extends JpaRepository<DomainRule, Long> {
    Optional<DomainRule> findByDomain(String domain);
}
