package com.github.robert2411.m3uplaylistfilter.service;

import com.github.robert2411.m3uplaylistfilter.entity.PlaylistEntry;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class M3uParserService {

    private static final Pattern ATTR_PATTERN = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");

    public void parse(InputStream inputStream, Consumer<PlaylistEntry> entryConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            PlaylistEntry pendingEntry = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTINF:")) {
                    pendingEntry = parseExtInf(line);
                } else if (!line.isEmpty() && !line.startsWith("#") && pendingEntry != null) {
                    pendingEntry.setStreamUrl(line);
                    pendingEntry.setStreamType(detectStreamType(line));
                    pendingEntry.setContainerExtension(extractExtension(line));
                    entryConsumer.accept(pendingEntry);
                    pendingEntry = null;
                }
            }
        }
    }

    private static String detectStreamType(String url) {
        if (url.contains("/movie/")) return "movie";
        if (url.contains("/series/")) return "series";
        return "live";
    }

    private static String extractExtension(String url) {
        int lastDot = url.lastIndexOf('.');
        int lastSlash = url.lastIndexOf('/');
        if (lastDot > lastSlash) {
            String ext = url.substring(lastDot + 1);
            if (ext.length() <= 5 && !ext.contains("?") && !ext.contains("&")) {
                return ext.toLowerCase();
            }
        }
        return null;
    }

    private PlaylistEntry parseExtInf(String line) {
        PlaylistEntry entry = new PlaylistEntry();
        Matcher attrMatcher = ATTR_PATTERN.matcher(line);
        while (attrMatcher.find()) {
            String key = attrMatcher.group(1);
            String value = attrMatcher.group(2);
            switch (key) {
                case "tvg-id" -> entry.setTvgId(value);
                case "tvg-logo" -> entry.setTvgLogo(value);
                case "group-title" -> entry.setGroupTitle(value);
                case "rating", "tvg-rating", "tmdb-rating" -> {
                    if (entry.getRating() == null) entry.setRating(value);
                }
            }
        }
        // Display name is everything after the last unquoted comma
        int lastQuotePos = line.lastIndexOf('"');
        int commaPos = line.indexOf(',', lastQuotePos >= 0 ? lastQuotePos : 0);
        if (commaPos >= 0) {
            entry.setName(line.substring(commaPos + 1).trim());
        }
        return entry;
    }
}
