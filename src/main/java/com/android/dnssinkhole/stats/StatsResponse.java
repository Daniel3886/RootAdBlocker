package com.android.dnssinkhole.stats;

record StatsResponse(int staticBlocklistSize, long totalQueriesLogged, long totalBlocked) {
}
