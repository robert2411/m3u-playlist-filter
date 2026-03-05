package com.github.robert2411.m3uplaylistfilter.service;

import com.github.robert2411.m3uplaylistfilter.entity.PlaylistEntry;
import com.github.robert2411.m3uplaylistfilter.repository.PlaylistEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@Service
public class PlaylistExportService {

    private final PlaylistEntryRepository entryRepo;

    public PlaylistExportService(PlaylistEntryRepository entryRepo) {
        this.entryRepo = entryRepo;
    }

    @Transactional(readOnly = true)
    public void streamPlaylist(OutputStream out) {
        PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8);
        try (Stream<PlaylistEntry> entries = entryRepo.streamIncluded()) {
            writer.println("#EXTM3U");
            entries.forEach(e -> {
                if (e.getStreamUrl() == null || e.getStreamUrl().isBlank()) return;
                StringBuilder extinf = new StringBuilder("#EXTINF:-1");
                if (e.getTvgId() != null && !e.getTvgId().isBlank()) {
                    extinf.append(" tvg-id=\"").append(e.getTvgId()).append("\"");
                }
                extinf.append(" tvg-name=\"").append(orEmpty(e.getName())).append("\"");
                if (e.getTvgLogo() != null && !e.getTvgLogo().isBlank()) {
                    extinf.append(" tvg-logo=\"").append(e.getTvgLogo()).append("\"");
                }
                extinf.append(" group-title=\"").append(orEmpty(e.getGroupTitle()))
                      .append("\",").append(orEmpty(e.getName()));
                writer.println(extinf);
                writer.println(e.getStreamUrl());
            });
        }
        writer.flush();
    }

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }
}
