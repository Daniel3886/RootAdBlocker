package com.android.dnssinkhole.blocklist;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DomainNames {

    private static final Pattern VALID_DOMAIN = Pattern.compile(
            "^(?=.{1,253}$)[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?(?:\\.[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?)*$");

    private DomainNames() {
    }

    public static String normalize(String domain) {
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static boolean isValid(String normalizedDomain) {
        return VALID_DOMAIN.matcher(normalizedDomain).matches();
    }
}
