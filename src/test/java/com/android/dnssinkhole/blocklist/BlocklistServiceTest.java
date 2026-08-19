package com.android.dnssinkhole.blocklist;

import com.android.dnssinkhole.model.DomainRule;
import com.android.dnssinkhole.repository.DomainRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlocklistServiceTest {

    @Mock
    private DomainRuleRepository domainRuleRepository;

    @TempDir
    private Path blocklistDir;

    private BlocklistService blocklistService;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException, IOException {
        blocklistService = new BlocklistService(domainRuleRepository);

        Files.writeString(blocklistDir.resolve("ads.txt"), """
                # Test blocklist
                0.0.0.0 googlesyndication.com
                0.0.0.0 ads.example.com
                """);
        Files.writeString(blocklistDir.resolve("trackers.txt"), """
                0.0.0.0 doubleclick.net
                0.0.0.0 44chan.me
                """);

        Field field = BlocklistService.class.getDeclaredField("blocklistDir");
        field.setAccessible(true);
        field.set(blocklistService, blocklistDir.toString());

        blocklistService.loadStaticList();
    }


    @Test
    void staticListContainsBlockedDomains() {
        assertTrue(blocklistService.isBlocked("googlesyndication.com"));
        assertTrue(blocklistService.isBlocked("doubleclick.net"));
        assertTrue(blocklistService.isBlocked("44chan.me"));
    }

    @Test
    void loadsDomainsFromAllFilesInDirectory() {
        assertTrue(blocklistService.isBlocked("ads.example.com"));
        assertEquals(4, blocklistService.staticListSize());
    }

    @Test
    void staticListDoesNotContainAllowedDomains() {
        assertFalse(blocklistService.isBlocked("localhost"));
        assertFalse(blocklistService.isBlocked("google.com"));
    }

    @Test
    void doesCaseInsensitiveMatching() {
        assertFalse(blocklistService.isBlocked("Google.com"));
        assertTrue(blocklistService.isBlocked("44CHAN.me"));
    }

    @Test
    void doesTrailingDotMatching() {
        assertTrue(blocklistService.isBlocked("googlesyndication.com."));
        assertTrue(blocklistService.isBlocked("doubleclick.net."));
        assertTrue(blocklistService.isBlocked("44chan.me."));
    }

    @Test
    void doesDBOverrideBlocking() {
        when(domainRuleRepository.findByDomain("google.com"))
                .thenReturn(Optional.of(new DomainRule("google.com", DomainRule.RuleType.BLOCK)));

        assertTrue(blocklistService.isBlocked("google.com"));
    }

    @Test
    void doesDBOverrideAllowing() {
        when(domainRuleRepository.findByDomain("doubleclick.net"))
                .thenReturn(Optional.of(new DomainRule("doubleclick.net", DomainRule.RuleType.ALLOW)));

        assertFalse(blocklistService.isBlocked("doubleclick.net"));
    }

}
