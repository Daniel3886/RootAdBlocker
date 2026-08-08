package com.android.dnssinkhole.repository;

import com.android.dnssinkhole.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
    List<QueryLog> findTop100ByOrderByTimestampDesc();
    long countByBlockedTrue();
}
