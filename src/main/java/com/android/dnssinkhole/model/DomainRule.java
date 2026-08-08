package com.android.dnssinkhole.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "domain_rules")
@Data
@RequiredArgsConstructor
public class DomainRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domain;

    // BLOCK or ALLOW (ALLOW overrides the static hosts-file blocklist)
    @Enumerated(EnumType.STRING)
    private RuleType type;

    public enum RuleType { BLOCK, ALLOW }
}
