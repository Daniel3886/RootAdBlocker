package com.android.dnssinkhole.stats;

import com.android.dnssinkhole.blocklist.BlocklistService;
import com.android.dnssinkhole.query.QueryLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
class StatsController {

    private final BlocklistService blocklistService;
    private final QueryLogService queryLogService;

    StatsController(BlocklistService blocklistService, QueryLogService queryLogService) {
        this.blocklistService = blocklistService;
        this.queryLogService = queryLogService;
    }

    @GetMapping
    StatsResponse stats() {
        return new StatsResponse(
                blocklistService.staticListSize(),
                queryLogService.totalQueries(),
                queryLogService.blockedQueries());
    }
}
