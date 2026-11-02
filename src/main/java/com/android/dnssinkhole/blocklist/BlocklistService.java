package com.android.dnssinkhole.blocklist;

import com.android.dnssinkhole.config.SinkholeProperties;
import com.android.dnssinkhole.model.DomainRule;
import com.android.dnssinkhole.repository.DomainRuleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

@Service
public class BlocklistService {

    private static final Logger log = LoggerFactory.getLogger(BlocklistService.class);

    private final DomainRuleRepository domainRuleRepository;
    private final Path blocklistPath;
    private final Map<String, DomainRule.RuleType> overrides = new ConcurrentHashMap<>();
    private volatile Set<String> staticBlockedDomains = Set.of();

    public BlocklistService(DomainRuleRepository domainRuleRepository, SinkholeProperties properties) {
        this.domainRuleRepository = domainRuleRepository;
        this.blocklistPath = Path.of(properties.blocklist().dir());
    }

    @PostConstruct
    public void init() {
        reloadStaticList();
        reloadOverrides();
    }

    public synchronized int reloadStaticList() {
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

    public void reloadOverrides() {
        Map<String, DomainRule.RuleType> fresh = new ConcurrentHashMap<>();
        domainRuleRepository.findAll().forEach(rule -> fresh.put(rule.getDomain(), rule.getType()));
        overrides.keySet().retainAll(fresh.keySet());
        overrides.putAll(fresh);
        log.info("Loaded {} block/allow rules from the database", fresh.size());
    }

    public void applyRule(DomainRule rule) {
        overrides.put(rule.getDomain(), rule.getType());
    }

    public void removeRule(String domain) {
        overrides.remove(domain);
    }

    public boolean isBlocked(String domain) {
        String normalized = DomainNames.normalize(domain);
        DomainRule.RuleType override = overrides.get(normalized);
        if (override != null) {
            return override == DomainRule.RuleType.BLOCK;
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
