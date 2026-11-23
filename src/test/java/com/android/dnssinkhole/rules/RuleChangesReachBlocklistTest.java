package com.android.dnssinkhole.rules;

import com.android.dnssinkhole.blocklist.BlocklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rule-events-test;DB_CLOSE_DELAY=-1",
        "sinkhole.listen-port=0",
        "sinkhole.blocklist.dir=target/no-blocklist"
})
class RuleChangesReachBlocklistTest {

    @Autowired
    private DomainRuleService domainRuleService;

    @Autowired
    private BlocklistService blocklistService;

    @Test
    void committedRuleChangesUpdateTheBlocklistCache() {
        assertFalse(blocklistService.isBlocked("tracker.example.org"));

        DomainRule rule = domainRuleService.upsert("tracker.example.org", RuleType.BLOCK);
        assertTrue(blocklistService.isBlocked("tracker.example.org"));

        domainRuleService.upsert("tracker.example.org", RuleType.ALLOW);
        assertFalse(blocklistService.isBlocked("tracker.example.org"));

        domainRuleService.upsert("tracker.example.org", RuleType.BLOCK);
        domainRuleService.delete(rule.getId());
        assertFalse(blocklistService.isBlocked("tracker.example.org"));
    }

}
