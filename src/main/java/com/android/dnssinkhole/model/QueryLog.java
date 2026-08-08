package com.android.dnssinkhole.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "query_log")
@Data
@NoArgsConstructor
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String domain;
    private String clientIp;
    private boolean blocked;
    private Instant timestamp;

    public QueryLog(String domain, String clientIp, boolean blocked) {
        this.domain = domain;
        this.clientIp = clientIp;
        this.blocked = blocked;
        this.timestamp = Instant.now();
    }
}
