package com.github.robert2411.m3uplaylistfilter.service;

import com.github.robert2411.m3uplaylistfilter.entity.FilterRule;
import com.github.robert2411.m3uplaylistfilter.entity.PlaylistEntry;
import com.github.robert2411.m3uplaylistfilter.entity.SourceConfig;
import com.github.robert2411.m3uplaylistfilter.repository.FilterRuleRepository;
import com.github.robert2411.m3uplaylistfilter.repository.PlaylistEntryRepository;
import com.github.robert2411.m3uplaylistfilter.repository.SourceConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class M3uImportService {

    private static final Logger log = LoggerFactory.getLogger(M3uImportService.class);
    private static final int BATCH_SIZE = 500;

    private final PlaylistEntryRepository entryRepo;
    private final FilterRuleRepository filterRepo;
    private final SourceConfigRepository sourceRepo;
    private final M3uParserService parser;

    public M3uImportService(PlaylistEntryRepository entryRepo,
                            FilterRuleRepository filterRepo,
                            SourceConfigRepository sourceRepo,
                            M3uParserService parser) {
        this.entryRepo = entryRepo;
        this.filterRepo = filterRepo;
        this.sourceRepo = sourceRepo;
        this.parser = parser;
    }

    @Transactional(rollbackFor = IOException.class)
    public void importPlaylist() throws IOException {
        long startTime = System.currentTimeMillis();
        SourceConfig config = sourceRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No source URL configured"));

        log.info("Starting playlist import from {}", config.getSourceUrl());
        entryRepo.deleteAllInBatch();
        log.info("Delete done");
        List<PlaylistEntry> batch = new ArrayList<>(BATCH_SIZE);
        parser.parse(openStream(config.getSourceUrl()), entry -> {
            batch.add(entry);
            if (batch.size() >= BATCH_SIZE) {
                entryRepo.saveAll(new ArrayList<>(batch));
                batch.clear();
            }
        });
        if (!batch.isEmpty()) {
            entryRepo.saveAll(batch);
        }
        long runtime = System.currentTimeMillis() - startTime;
        log.info("Playlist import completed: {} entries imported in {}ms", entryRepo.count(), runtime);

        Set<String> existingGroups = filterRepo.findAll().stream()
                .map(FilterRule::getGroupTitle)
                .collect(Collectors.toSet());

        entryRepo.countByGroupTitle().stream()
                .map(row -> (String) row[0])
                .filter(g -> g != null && !existingGroups.contains(g))
                .map(g -> {
                    FilterRule rule = new FilterRule();
                    rule.setGroupTitle(g);
                    rule.setIncluded(true);
                    return rule;
                })
                .forEach(filterRepo::save);

        config.setLastImported(LocalDateTime.now());
        sourceRepo.save(config);
        long totalRuntime = System.currentTimeMillis() - startTime;
        log.info("Playlist import completed: {} entries imported in {}ms", entryRepo.count(), totalRuntime);
    }

    private InputStream openStream(String url) throws IOException {
        String current = url;
        for (int redirects = 0; redirects < 10; redirects++) {
            HttpURLConnection conn = (HttpURLConnection) URI.create(current).toURL().openConnection();
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(300_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setInstanceFollowRedirects(false);
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == 307 || status == 308) {
                current = conn.getHeaderField("Location");
                conn.disconnect();
                continue;
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + status + " for URL: " + current);
            }
            return conn.getInputStream();
        }
        throw new IOException("Too many redirects for URL: " + url);
    }
}
