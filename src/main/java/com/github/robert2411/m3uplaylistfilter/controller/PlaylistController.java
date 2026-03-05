package com.github.robert2411.m3uplaylistfilter.controller;

import com.github.robert2411.m3uplaylistfilter.service.PlaylistExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.ByteArrayOutputStream;

@Controller
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);

    private final PlaylistExportService exportService;

    public PlaylistController(PlaylistExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/playlist.m3u")
    public ResponseEntity<byte[]> playlist(HttpServletRequest request) {
        log.info("Playlist requested by {}", request.getRemoteAddr());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        exportService.streamPlaylist(buffer);
        byte[] content = buffer.toByteArray();
        log.info("Serving playlist: {} bytes, {} KB", content.length, content.length / 1024);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/x-mpegurl")
                .contentLength(content.length)
                .body(content);
    }
}
