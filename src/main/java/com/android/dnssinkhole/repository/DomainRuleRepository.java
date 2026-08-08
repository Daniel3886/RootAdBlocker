package com.android.dnssinkhole.repository;


import com.android.dnssinkhole.model.DomainRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DomainRuleRepository extends JpaRepository<DomainRule, Long> {
    Optional<DomainRule> findByDomain(String domain);
}
