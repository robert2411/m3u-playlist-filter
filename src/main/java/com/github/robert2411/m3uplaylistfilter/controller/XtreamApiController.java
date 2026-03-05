package com.github.robert2411.m3uplaylistfilter.controller;

import com.github.robert2411.m3uplaylistfilter.entity.FilterRule;
import com.github.robert2411.m3uplaylistfilter.entity.PlaylistEntry;
import com.github.robert2411.m3uplaylistfilter.repository.FilterRuleRepository;
import com.github.robert2411.m3uplaylistfilter.repository.PlaylistEntryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class XtreamApiController {

    private static final Logger log = LoggerFactory.getLogger(XtreamApiController.class);

    @Value("${xtream.username}")
    private String xtreamUsername;

    @Value("${xtream.password}")
    private String xtreamPassword;

    private final FilterRuleRepository filterRepo;
    private final PlaylistEntryRepository entryRepo;

    public XtreamApiController(FilterRuleRepository filterRepo,
                               PlaylistEntryRepository entryRepo) {
        this.filterRepo = filterRepo;
        this.entryRepo = entryRepo;
    }

    @GetMapping("/player_api.php")
    @PostMapping("/player_api.php")
    public Object playerApi(HttpServletRequest request,
                            @RequestParam String username,
                            @RequestParam String password,
                            @RequestParam(required = false) String action,
                            @RequestParam(required = false) String category_id) {
        log.info("Xtream API request from {} — action={}, category_id={}, username={}", request.getRemoteAddr(), action, category_id, username);
        if (!authenticate(username, password)) {
            log.warn("Xtream API auth failed for username={} from {}", username, request.getRemoteAddr());
            return Map.of("user_info", buildFailedUserInfo(username, password),
                          "server_info", buildServerInfo(request, username, password).get("server_info"));
        }
        if (action == null) {
            return buildServerInfo(request, username, password);
        }
        return switch (action) {
            case "get_live_categories"   -> buildCategories("live");
            case "get_live_streams"      -> buildStreams(category_id, "live");
            case "get_vod_categories"    -> buildCategories("movie");
            case "get_vod_streams"       -> buildStreams(category_id, "movie");
            case "get_series_categories" -> buildCategories("series");
            case "get_series"            -> buildStreams(category_id, "series");
            default -> Map.of();
        };
    }

    /** Stream redirect: TV app requests /live/user/pass/ID.ts → 302 to original URL */
    @GetMapping("/live/{username}/{password}/{streamRef:.+}")
    public ResponseEntity<Void> streamRedirect(@PathVariable String streamRef) {
        return redirectToStream(streamRef);
    }

    @GetMapping("/movie/{username}/{password}/{streamRef:.+}")
    public ResponseEntity<Void> movieRedirect(@PathVariable String streamRef) {
        return redirectToStream(streamRef);
    }

    @GetMapping("/series/{username}/{password}/{streamRef:.+}")
    public ResponseEntity<Void> seriesRedirect(@PathVariable String streamRef) {
        return redirectToStream(streamRef);
    }

    // --- private helpers ---

    private ResponseEntity<Void> redirectToStream(String streamRef) {
        log.info("Xtream stream request — streamRef={}", streamRef);
        try {
            String idStr = streamRef.contains(".")
                    ? streamRef.substring(0, streamRef.lastIndexOf('.'))
                    : streamRef;
            long id = Long.parseLong(idStr);
            return entryRepo.findById(id)
                    .filter(e -> e.getStreamUrl() != null && !e.getStreamUrl().isBlank())
                    .map(e -> ResponseEntity.status(302).header("Location", e.getStreamUrl()).<Void>build())
                    .orElse(ResponseEntity.notFound().<Void>build());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().<Void>build();
        }
    }

    private Map<String, Object> buildFailedUserInfo(String username, String password) {
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("username", username);
        userInfo.put("password", password);
        userInfo.put("message", "Wrong username or password!");
        userInfo.put("auth", 0);
        userInfo.put("status", "Disabled");
        userInfo.put("exp_date", 0);
        userInfo.put("is_trial", "0");
        userInfo.put("active_cons", "0");
        userInfo.put("created_at", "0");
        userInfo.put("max_connections", "0");
        userInfo.put("allowed_output_formats", List.of("ts"));
        return userInfo;
    }

    private boolean authenticate(String username, String password) {
        return xtreamUsername.equals(username) && xtreamPassword.equals(password);
    }

    private Map<String, Object> buildServerInfo(HttpServletRequest req, String username, String password) {
        String host = req.getServerName();
        String port = String.valueOf(req.getServerPort());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("username", username);
        userInfo.put("password", password);
        userInfo.put("message", "Welcome");
        userInfo.put("auth", 1);
        userInfo.put("status", "Active");
        userInfo.put("exp_date", 0);
        userInfo.put("is_trial", "0");
        userInfo.put("active_cons", "1");
        userInfo.put("created_at", String.valueOf(Instant.now().getEpochSecond()));
        userInfo.put("max_connections", "999");
        userInfo.put("allowed_output_formats", List.of("ts", "m3u8"));

        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("xui", true);
        serverInfo.put("version", "1.5.12");
        serverInfo.put("url", host);
        serverInfo.put("port", port);
        serverInfo.put("https_port", port);
        serverInfo.put("server_protocol", "http");
        serverInfo.put("rtmp_port", port);
        serverInfo.put("timezone", "UTC");
        serverInfo.put("timestamp_now", Instant.now().getEpochSecond());
        serverInfo.put("time_now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return Map.of("user_info", userInfo, "server_info", serverInfo);
    }

    private List<Map<String, Object>> buildCategories(String streamType) {
        Set<String> groupsWithType = entryRepo.findAllIncludedByType(streamType).stream()
                .map(PlaylistEntry::getGroupTitle)
                .collect(Collectors.toSet());
        return filterRepo.findAll().stream()
                .filter(FilterRule::isIncluded)
                .filter(r -> groupsWithType.contains(r.getGroupTitle()))
                .sorted(Comparator.comparing(FilterRule::getGroupTitle))
                .map(r -> {
                    Map<String, Object> cat = new LinkedHashMap<>();
                    cat.put("category_id", String.valueOf(r.getId()));
                    cat.put("category_name", r.getGroupTitle());
                    cat.put("parent_id", 0);
                    return cat;
                })
                .toList();
    }

    private List<Map<String, Object>> buildStreams(String categoryId, String streamType) {
        List<PlaylistEntry> entries;
        Map<String, Long> groupToCategoryId;

        if (categoryId != null && !categoryId.isBlank()) {
            try {
                long ruleId = Long.parseLong(categoryId);
                FilterRule rule = filterRepo.findById(ruleId)
                        .filter(FilterRule::isIncluded)
                        .orElse(null);
                if (rule == null) return Collections.emptyList();
                entries = entryRepo.findByGroupTitleAndType(rule.getGroupTitle(), streamType);
                groupToCategoryId = Map.of(rule.getGroupTitle(), rule.getId());
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }
        } else {
            entries = entryRepo.findAllIncludedByType(streamType);
            groupToCategoryId = filterRepo.findAll().stream()
                    .filter(FilterRule::isIncluded)
                    .collect(Collectors.toMap(FilterRule::getGroupTitle, FilterRule::getId));
        }

        int[] num = {1};
        return entries.stream()
                .map(e -> {
                    String catIdStr = String.valueOf(
                            groupToCategoryId.getOrDefault(e.getGroupTitle(), 0L));
                    Map<String, Object> stream = new LinkedHashMap<>();
                    stream.put("num", num[0]++);
                    stream.put("name", e.getName() != null ? e.getName() : "");
                    stream.put("stream_type", streamType);
                    stream.put("stream_id", e.getId());
                    stream.put("stream_icon", e.getTvgLogo() != null ? e.getTvgLogo() : "");
                    stream.put("epg_channel_id", e.getTvgId() != null ? e.getTvgId() : "");
                    stream.put("added", "1000000000");
                    stream.put("is_adult", "0");
                    stream.put("category_id", catIdStr);
                    stream.put("category_ids", List.of(catIdStr));
                    stream.put("custom_sid", "");
                    stream.put("tv_archive", 0);
                    stream.put("direct_source", "");
                    stream.put("tv_archive_duration", 0);
                    return stream;
                })
                .toList();
    }

    private static double toRating5(String rating) {
        if (rating == null || rating.isBlank()) return 0.0;
        try {
            double r = Double.parseDouble(rating);
            // If rating looks like it's out of 10, halve it
            return r > 5.0 ? Math.round((r / 2.0) * 10.0) / 10.0 : r;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
