package com.android.dnssinkhole.blocklist;

import com.android.dnssinkhole.common.DomainNames;
import com.android.dnssinkhole.config.SinkholeProperties;
import com.android.dnssinkhole.rules.DomainRuleDeletedEvent;
import com.android.dnssinkhole.rules.DomainRuleSavedEvent;
import com.android.dnssinkhole.rules.DomainRuleService;
import com.android.dnssinkhole.rules.RuleType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Service
public class BlocklistService {

    private final DomainRuleService domainRuleService;
    private final Path blocklistPath;
    private final Map<String, RuleType> overrides = new ConcurrentHashMap<>();
    private volatile Set<String> staticBlockedDomains = Set.of();

    BlocklistService(DomainRuleService domainRuleService, SinkholeProperties properties) {
        this.domainRuleService = domainRuleService;
        this.blocklistPath = Path.of(properties.blocklist().dir());
    }

    @PostConstruct
    void init() {
        reloadStaticList();
        reloadOverrides();
    }

    synchronized int reloadStaticList() {
        Path path = blocklistPath.toAbsolutePath();
        if (!Files.exists(path)) {
            log.warn("Blocklist path not found at {}. Sinkhole will only use DB rules until you add one.", path);
            staticBlockedDomains = Set.of();
            return 0;
        }

        Set<String> domains = new HashSet<>();
        try {
            if (Files.isDirectory(path)) {
                loadDirectory(path, domains);
            } else if (Files.isRegularFile(path)) {
                loadFile(path, domains);
            } else {
                log.warn("Blocklist path at {} is not a regular file or directory.", path);
            }
        } catch (IOException e) {
            log.error("Failed to read blocklist at {}. Keeping the previous list.", path, e);
            return staticBlockedDomains.size();
        }

        staticBlockedDomains = Collections.unmodifiableSet(domains);
        log.info("Loaded {} domains into the static blocklist from {}", domains.size(), path);
        return domains.size();
    }

    void reloadOverrides() {
        Map<String, RuleType> fresh = domainRuleService.currentRules();
        overrides.keySet().retainAll(fresh.keySet());
        overrides.putAll(fresh);
        log.info("Loaded {} block/allow rules from the database", fresh.size());
    }

    // AFTER_COMMIT: a rule that rolls back must never reach the in-memory cache.
    @TransactionalEventListener
    void onRuleSaved(DomainRuleSavedEvent event) {
        overrides.put(event.domain(), event.type());
    }

    @TransactionalEventListener
    void onRuleDeleted(DomainRuleDeletedEvent event) {
        overrides.remove(event.domain());
    }

    public boolean isBlocked(String domain) {
        String normalized = DomainNames.normalize(domain);
        RuleType override = overrides.get(normalized);
        if (override != null) {
            return override == RuleType.BLOCK;
        }
        return staticBlockedDomains.contains(normalized);
    }

    public int staticListSize() {
        return staticBlockedDomains.size();
    }

    private void loadDirectory(Path directory, Set<String> domains) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            Iterator<Path> iterator = files.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                loadFile(iterator.next(), domains);
            }
        }
    }

    private void loadFile(Path file, Set<String> domains) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseHostsLine(line, domains);
            }
        }
    }

    private void parseHostsLine(String line, Set<String> domains) {
        int commentStart = line.indexOf('#');
        String content = (commentStart >= 0 ? line.substring(0, commentStart) : line).trim();
        if (content.isEmpty()) {
            return;
        }

        String[] parts = content.split("\\s+");
        for (int i = 1; i < parts.length; i++) {
            String domain = DomainNames.normalize(parts[i]);
            if (!isLocalName(domain)) {
                domains.add(domain);
            }
        }
    }

    private static boolean isLocalName(String domain) {
        return domain.equals("localhost")
                || domain.equals("localhost.localdomain")
                || domain.equals("local")
                || domain.equals("broadcasthost")
                || domain.equals("0.0.0.0")
                || domain.startsWith("ip6-");
    }
}
