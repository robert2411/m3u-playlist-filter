package com.github.robert2411.m3uplaylistfilter.controller;

import com.github.robert2411.m3uplaylistfilter.entity.SourceConfig;
import com.github.robert2411.m3uplaylistfilter.repository.SourceConfigRepository;
import com.github.robert2411.m3uplaylistfilter.service.M3uImportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingsController {

    @Value("${xtream.username}")
    private String xtreamUsername;

    @Value("${xtream.password}")
    private String xtreamPassword;

    private final SourceConfigRepository sourceRepo;
    private final M3uImportService importService;

    public SettingsController(SourceConfigRepository sourceRepo, M3uImportService importService) {
        this.sourceRepo = sourceRepo;
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
}
