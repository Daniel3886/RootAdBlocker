package com.android.dnssinkhole.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "domain_rules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DomainRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Enumerated(EnumType.STRING)
    private RuleType type;

    public DomainRule(String domain, RuleType ruleType) {
        this.domain = domain;
        this.type = ruleType;
    }

    public enum RuleType { BLOCK, ALLOW }
}
