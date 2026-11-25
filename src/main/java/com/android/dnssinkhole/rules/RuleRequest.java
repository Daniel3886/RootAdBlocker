package com.android.dnssinkhole.rules;

import jakarta.validation.constraints.NotBlank;

record RuleRequest(@NotBlank String domain) {
}
