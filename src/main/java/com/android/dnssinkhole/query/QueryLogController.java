package com.android.dnssinkhole.query;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
class QueryLogController {

    private final QueryLogService queryLogService;

    QueryLogController(QueryLogService queryLogService) {
        this.queryLogService = queryLogService;
    }

    @GetMapping
    List<QueryLog> recentLogs() {
        return queryLogService.recent();
    }
}
