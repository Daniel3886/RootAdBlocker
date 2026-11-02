package com.android.dnssinkhole.repository;

import com.android.dnssinkhole.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
    List<QueryLog> findTop100ByOrderByTimestampDesc();

    long countByBlockedTrue();

    @Modifying
    @Transactional
    @Query("delete from QueryLog q where q.timestamp < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
