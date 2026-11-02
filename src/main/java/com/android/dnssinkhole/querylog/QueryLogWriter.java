package com.android.dnssinkhole.querylog;

import com.android.dnssinkhole.config.SinkholeProperties;
import com.android.dnssinkhole.model.QueryLog;
import com.android.dnssinkhole.repository.QueryLogRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class QueryLogWriter {

    private static final Logger log = LoggerFactory.getLogger(QueryLogWriter.class);
    private static final int MAX_BATCH_SIZE = 500;

    private final QueryLogRepository queryLogRepository;
    private final BlockingQueue<QueryLog> buffer;
    private final int retentionDays;
    private final AtomicLong dropped = new AtomicLong();

    public QueryLogWriter(QueryLogRepository queryLogRepository, SinkholeProperties properties) {
        this.queryLogRepository = queryLogRepository;
        this.buffer = new ArrayBlockingQueue<>(properties.queryLog().bufferCapacity());
        this.retentionDays = properties.queryLog().retentionDays();
    }

    public void record(String domain, String clientIp, boolean blocked) {
        if (!buffer.offer(new QueryLog(domain, clientIp, blocked))) {
            dropped.incrementAndGet();
        }
    }

    @Scheduled(fixedDelayString = "${sinkhole.query-log.flush-interval-ms:2000}")
    public void flush() {
        List<QueryLog> batch = new ArrayList<>(MAX_BATCH_SIZE);
        while (buffer.drainTo(batch, MAX_BATCH_SIZE) > 0) {
            try {
                queryLogRepository.saveAll(batch);
            } catch (RuntimeException e) {
                log.error("Failed to write {} query log entries", batch.size(), e);
                return;
            } finally {
                batch.clear();
            }
        }

        long droppedCount = dropped.getAndSet(0);
        if (droppedCount > 0) {
            log.warn("Query log buffer was full; dropped {} entries", droppedCount);
        }
    }

    @Scheduled(cron = "${sinkhole.query-log.cleanup-cron:0 0 4 * * *}")
    public void purgeOldEntries() {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = queryLogRepository.deleteOlderThan(cutoff);
        log.info("Deleted {} query log entries older than {} days", deleted, retentionDays);
    }

    @PreDestroy
    public void shutdown() {
        flush();
    }
}
