package com.android.dnssinkhole.rules;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
class RuleController {

    private final DomainRuleService domainRuleService;

    RuleController(DomainRuleService domainRuleService) {
        this.domainRuleService = domainRuleService;
    }

    @GetMapping
    List<DomainRule> rules() {
        return domainRuleService.findAll();
    }

    @PostMapping("/block")
    DomainRule addBlockRule(@Valid @RequestBody RuleRequest request) {
        return domainRuleService.upsert(request.domain(), RuleType.BLOCK);
    }

    @PostMapping("/allow")
    DomainRule addAllowRule(@Valid @RequestBody RuleRequest request) {
        return domainRuleService.upsert(request.domain(), RuleType.ALLOW);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteRule(@PathVariable Long id) {
        domainRuleService.delete(id);
    }
}
