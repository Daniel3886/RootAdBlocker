package com.android.dnssinkhole.blocklist;


import com.android.dnssinkhole.model.DomainRule;
import com.android.dnssinkhole.repository.DomainRuleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Fast path: a big static hosts-style blocklist loaded into memory at startup.
 * Slow/flexible path: user-added BLOCK or ALLOW rules stored in the DB,
 * checked after the in-memory set and able to override it (ALLOW wins).
 */
@Service
public class BlocklistService {

    private static final Logger log = LoggerFactory.getLogger(BlocklistService.class);

    // Local cache of the hosts-file style list. Grab one from:
    // https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts
    @Value("${sinkhole.blocklist.dir:${sinkhole.blocklist.file:blocklist}}")
    private String blocklistDir;

    private final Set<String> staticBlockedDomains = ConcurrentHashMap.newKeySet();
    private final DomainRuleRepository domainRuleRepository;

    public BlocklistService(DomainRuleRepository domainRuleRepository) {
        this.domainRuleRepository = domainRuleRepository;
    }

    @PostConstruct
    public void loadStaticList() {
        Path path = Path.of(blocklistDir);
        if (!Files.exists(path)) {
            log.warn("Blocklist path not found at {}. Sinkhole will only use DB rules until you add one.", path.toAbsolutePath());
            return;
        }

        staticBlockedDomains.clear();
        try {
            int count;
            if (Files.isDirectory(path)) {
                count = loadBlocklistDirectory(path);
                log.info("Loaded {} domains into the static blocklist from files under {}", count, path.toAbsolutePath());
            } else if (Files.isRegularFile(path)) {
                count = loadBlocklistFile(path);
                log.info("Loaded {} domains into the static blocklist from {}", count, path.toAbsolutePath());
            } else {
                log.warn("Blocklist path at {} is not a regular file or directory.", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to read blocklist path", e);
        }
    }

    private int loadBlocklistDirectory(Path directory) throws IOException {
        int count = 0;
        try (Stream<Path> files = Files.walk(directory)) {
            Iterator<Path> iterator = files.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                count += loadBlocklistFile(iterator.next());
            }
        }
        return count;
    }

    private int loadBlocklistFile(Path file) throws IOException {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // hosts file lines look like: "0.0.0.0 ads.example.com"
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String domain = parts[1].toLowerCase();
                    if (!domain.equals("localhost") && staticBlockedDomains.add(domain)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Returns true if the domain should be sinkholed.
     * DB "ALLOW" rules always win over the static list (manual whitelist).
     * DB "BLOCK" rules add to the static list without needing a re-download.
     */
    public boolean isBlocked(String domain) {
        String normalized = domain.toLowerCase();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        var override = domainRuleRepository.findByDomain(normalized);
        if (override.isPresent()) {
            return override.get().getType() == DomainRule.RuleType.BLOCK;
        }

        return staticBlockedDomains.contains(normalized);
    }

    public int staticListSize() {
        return staticBlockedDomains.size();
    }
}
