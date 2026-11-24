package com.android.dnssinkhole.rules;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "domain_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DomainRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;

    DomainRule(String domain, RuleType type) {
        this.domain = Objects.requireNonNull(domain, "domain");
        this.type = Objects.requireNonNull(type, "type");
    }

    void changeTypeTo(RuleType newType) {
        this.type = Objects.requireNonNull(newType, "type");
    }
}
