package com.android.dnssinkhole.web;

public record StatsResponse(int staticBlocklistSize, long totalQueriesLogged, long totalBlocked) {
}
