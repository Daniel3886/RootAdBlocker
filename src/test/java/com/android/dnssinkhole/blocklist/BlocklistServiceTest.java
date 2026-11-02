package com.android.dnssinkhole.blocklist;

import com.android.dnssinkhole.config.TestProperties;
import com.android.dnssinkhole.model.DomainRule;
import com.android.dnssinkhole.repository.DomainRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlocklistServiceTest {

    @Mock
    private DomainRuleRepository domainRuleRepository;

    @TempDir
    private Path blocklistDir;

    private BlocklistService blocklistService;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(blocklistDir.resolve("ads.txt"), """
                # Test blocklist
                127.0.0.1 localhost
                0.0.0.0 0.0.0.0
                0.0.0.0 googlesyndication.com
                0.0.0.0 ads.example.com # inline comment
                """);
        Files.writeString(blocklistDir.resolve("trackers.txt"), """
                0.0.0.0 doubleclick.net
                0.0.0.0 44chan.me
                """);

        blocklistService = new BlocklistService(domainRuleRepository,
                TestProperties.withBlocklistDir(blocklistDir.toString()));
    }

    private void initWithRules(DomainRule... rules) {
        when(domainRuleRepository.findAll()).thenReturn(List.of(rules));
        blocklistService.init();
    }

    @Test
    void staticListContainsBlockedDomains() {
        initWithRules();
        assertTrue(blocklistService.isBlocked("googlesyndication.com"));
        assertTrue(blocklistService.isBlocked("doubleclick.net"));
        assertTrue(blocklistService.isBlocked("44chan.me"));
    }

    @Test
    void loadsDomainsFromAllFilesInDirectory() {
        initWithRules();
        assertTrue(blocklistService.isBlocked("ads.example.com"));
        assertEquals(4, blocklistService.staticListSize());
    }

    @Test
    void staticListDoesNotContainAllowedDomains() {
        initWithRules();
        assertFalse(blocklistService.isBlocked("localhost"));
        assertFalse(blocklistService.isBlocked("0.0.0.0"));
        assertFalse(blocklistService.isBlocked("google.com"));
    }

    @Test
    void doesCaseInsensitiveMatching() {
        initWithRules();
        assertFalse(blocklistService.isBlocked("Google.com"));
        assertTrue(blocklistService.isBlocked("44CHAN.me"));
    }

    @Test
    void doesTrailingDotMatching() {
        initWithRules();
        assertTrue(blocklistService.isBlocked("googlesyndication.com."));
        assertTrue(blocklistService.isBlocked("doubleclick.net."));
        assertTrue(blocklistService.isBlocked("44chan.me."));
    }

    @Test
    void doesDBOverrideBlocking() {
        initWithRules(new DomainRule("google.com", DomainRule.RuleType.BLOCK));
        assertTrue(blocklistService.isBlocked("google.com"));
    }

    @Test
    void doesDBOverrideAllowing() {
        initWithRules(new DomainRule("doubleclick.net", DomainRule.RuleType.ALLOW));
        assertFalse(blocklistService.isBlocked("doubleclick.net"));
    }

    @Test
    void appliesAndRemovesRulesWithoutReload() {
        initWithRules();

        blocklistService.applyRule(new DomainRule("doubleclick.net", DomainRule.RuleType.ALLOW));
        assertFalse(blocklistService.isBlocked("doubleclick.net"));

        blocklistService.removeRule("doubleclick.net");
        assertTrue(blocklistService.isBlocked("doubleclick.net"));
    }

    @Test
    void reloadPicksUpNewFiles() throws IOException {
        initWithRules();
        Files.writeString(blocklistDir.resolve("extra.txt"), "0.0.0.0 tracker.example.org\n");

        assertEquals(5, blocklistService.reloadStaticList());
        assertTrue(blocklistService.isBlocked("tracker.example.org"));
    }
}
