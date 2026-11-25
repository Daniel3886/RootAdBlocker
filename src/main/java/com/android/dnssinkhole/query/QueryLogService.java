package com.android.dnssinkhole.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class QueryLogService {

    private final QueryLogRepository queryLogRepository;

    QueryLogService(QueryLogRepository queryLogRepository) {
        this.queryLogRepository = queryLogRepository;
    }

    public long totalQueries() {
        return queryLogRepository.count();
    }

    public long blockedQueries() {
        return queryLogRepository.countByBlockedTrue();
    }

    List<QueryLog> recent() {
        return queryLogRepository.findTop100ByOrderByTimestampDesc();
    }
}
