package com.events.platform.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SlugService {

    private static final Pattern NON_LATIN = Pattern.compile("[^a-z0-9-]");

    public String slugify(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String normalized =
                Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "");
        normalized = NON_LATIN.matcher(normalized.replace(' ', '-')).replaceAll("");
        normalized = normalized.replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized.substring(0, Math.min(normalized.length(), 80));
    }
}
