package com.android.dnssinkhole.web;

import jakarta.validation.constraints.NotBlank;

public record RuleRequest(@NotBlank String domain) {
}
