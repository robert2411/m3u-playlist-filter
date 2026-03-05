package com.github.robert2411.m3uplaylistfilter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.robert2411.m3uplaylistfilter.entity.FilterRule;
import com.github.robert2411.m3uplaylistfilter.entity.SourceConfig;
import com.github.robert2411.m3uplaylistfilter.repository.FilterRuleRepository;
import com.github.robert2411.m3uplaylistfilter.repository.SourceConfigRepository;
import com.github.robert2411.m3uplaylistfilter.service.M3uImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SettingsController {

    @Value("${xtream.username}")
    private String xtreamUsername;

    @Value("${xtream.password}")
    private String xtreamPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SourceConfigRepository sourceRepo;
    private final FilterRuleRepository filterRepo;
    private final M3uImportService importService;

    public SettingsController(SourceConfigRepository sourceRepo,
                              FilterRuleRepository filterRepo,
                              M3uImportService importService) {
        this.sourceRepo = sourceRepo;
        this.filterRepo = filterRepo;
        this.importService = importService;
    }

    @GetMapping("/settings")
    public String settings(Model model, HttpServletRequest request) {
        SourceConfig config = sourceRepo.findAll().stream().findFirst()
                .orElse(new SourceConfig());
        model.addAttribute("config", config);

        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        model.addAttribute("xtreamUrl", base + "/player_api.php?username=" + xtreamUsername + "&password=" + xtreamPassword);

        return "settings";
    }

    @PostMapping("/settings/save")
    public String save(@RequestParam String sourceUrl, RedirectAttributes ra) {
        SourceConfig config = sourceRepo.findAll().stream().findFirst()
                .orElse(new SourceConfig());
        config.setSourceUrl(sourceUrl);
        sourceRepo.save(config);
        ra.addFlashAttribute("message", "Source URL saved.");
        return "redirect:/settings";
    }

    @PostMapping("/settings/import")
    public String importPlaylist(RedirectAttributes ra) {
        try {
            importService.importPlaylist();
            ra.addFlashAttribute("message", "Import completed successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Import failed: " + e.getMessage());
        }
        return "redirect:/settings";
    }

    @GetMapping("/settings/export")
    public ResponseEntity<byte[]> export() throws Exception {
        SourceConfig config = sourceRepo.findAll().stream().findFirst()
                .orElse(new SourceConfig());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sourceUrl", config.getSourceUrl());
        data.put("categories", filterRepo.findAllByOrderByGroupTitleAsc().stream()
                .map(r -> Map.of("groupTitle", r.getGroupTitle(), "included", r.isIncluded()))
                .collect(Collectors.toList()));

        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"m3u-filter-settings.json\"")
                .body(json);
    }

    @PostMapping("/settings/import-config")
    public String importConfig(@RequestParam MultipartFile file, RedirectAttributes ra) {
        try {
            Map<?, ?> data = objectMapper.readValue(file.getInputStream(), Map.class);

            if (data.containsKey("sourceUrl") && data.get("sourceUrl") != null) {
                SourceConfig config = sourceRepo.findAll().stream().findFirst()
                        .orElse(new SourceConfig());
                config.setSourceUrl(data.get("sourceUrl").toString());
                sourceRepo.save(config);
            }

            if (data.containsKey("categories")) {
                List<?> categories = (List<?>) data.get("categories");
                Map<String, FilterRule> rulesByTitle = filterRepo.findAll().stream()
                        .collect(Collectors.toMap(FilterRule::getGroupTitle, r -> r));

                for (Object cat : categories) {
                    Map<?, ?> entry = (Map<?, ?>) cat;
                    String groupTitle = entry.get("groupTitle").toString();
                    boolean included = Boolean.parseBoolean(entry.get("included").toString());
                    FilterRule rule = rulesByTitle.getOrDefault(groupTitle, new FilterRule());
                    rule.setGroupTitle(groupTitle);
                    rule.setIncluded(included);
                    filterRepo.save(rule);
                }
            }

            ra.addFlashAttribute("message", "Settings imported successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Import failed: " + e.getMessage());
        }
        return "redirect:/settings";
    }
}
