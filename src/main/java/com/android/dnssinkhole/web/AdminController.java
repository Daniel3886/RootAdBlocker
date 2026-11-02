package com.android.dnssinkhole.web;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.model.DomainRule;
import com.android.dnssinkhole.model.QueryLog;
import com.android.dnssinkhole.repository.QueryLogRepository;
import com.android.dnssinkhole.rules.DomainRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {

    private final DomainRuleService domainRuleService;
    private final QueryLogRepository queryLogRepository;
    private final BlocklistService blocklistService;

    public AdminController(DomainRuleService domainRuleService,
                           QueryLogRepository queryLogRepository,
                           BlocklistService blocklistService) {
        this.domainRuleService = domainRuleService;
        this.queryLogRepository = queryLogRepository;
        this.blocklistService = blocklistService;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return new StatsResponse(
                blocklistService.staticListSize(),
                queryLogRepository.count(),
                queryLogRepository.countByBlockedTrue());
    }

    @GetMapping("/logs")
    public List<QueryLog> recentLogs() {
        return queryLogRepository.findTop100ByOrderByTimestampDesc();
    }

    @GetMapping("/rules")
    public List<DomainRule> rules() {
        return domainRuleService.findAll();
    }

    @PostMapping("/rules/block")
    public DomainRule addBlockRule(@Valid @RequestBody RuleRequest request) {
        return domainRuleService.upsert(request.domain(), DomainRule.RuleType.BLOCK);
    }

    @PostMapping("/rules/allow")
    public DomainRule addAllowRule(@Valid @RequestBody RuleRequest request) {
        return domainRuleService.upsert(request.domain(), DomainRule.RuleType.ALLOW);
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        domainRuleService.delete(id);
    }

    @GetMapping("/blocklist/check")
    public Map<String, Object> checkDomain(@RequestParam String domain) {
        return Map.of("domain", domain, "blocked", blocklistService.isBlocked(domain));
    }

    @PostMapping("/blocklist/reload")
    public Map<String, Object> reloadBlocklist() {
        return Map.of("staticBlocklistSize", blocklistService.reloadStaticList());
    }
}
