package com.android.dnssinkhole.web;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.model.DomainRule;
import com.android.dnssinkhole.repository.DomainRuleRepository;
import com.android.dnssinkhole.repository.QueryLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {

    private final DomainRuleRepository domainRuleRepository;
    private final QueryLogRepository queryLogRepository;
    private final BlocklistService blocklistService;

    public AdminController(DomainRuleRepository domainRuleRepository,
                            QueryLogRepository queryLogRepository,
                            BlocklistService blocklistService) {
        this.domainRuleRepository = domainRuleRepository;
        this.queryLogRepository = queryLogRepository;
        this.blocklistService = blocklistService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "staticBlocklistSize", blocklistService.staticListSize(),
                "totalQueriesLogged", queryLogRepository.count(),
                "totalBlocked", queryLogRepository.countByBlockedTrue()
        );
    }

    @GetMapping("/logs")
    public List<?> recentLogs() {
        return queryLogRepository.findTop100ByOrderByTimestampDesc();
    }

    @GetMapping("/rules")
    public List<DomainRule> rules() {
        return domainRuleRepository.findAll();
    }

    @PostMapping("/rules/block")
    public DomainRule addBlockRule(@RequestBody Map<String, String> body) {
        return domainRuleRepository.save(new DomainRule(body.get("domain").toLowerCase(), DomainRule.RuleType.BLOCK));
    }

    @PostMapping("/rules/allow")
    public DomainRule addAllowRule(@RequestBody Map<String, String> body) {
        return domainRuleRepository.save(new DomainRule(body.get("domain").toLowerCase(), DomainRule.RuleType.ALLOW));
    }

    @DeleteMapping("/rules/{id}")
    public void deleteRule(@PathVariable Long id) {
        domainRuleRepository.deleteById(id);
    }
}
