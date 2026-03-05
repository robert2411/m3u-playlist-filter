package com.github.robert2411.m3uplaylistfilter.controller;

import com.github.robert2411.m3uplaylistfilter.repository.FilterRuleRepository;
import com.github.robert2411.m3uplaylistfilter.repository.PlaylistEntryRepository;
import com.github.robert2411.m3uplaylistfilter.repository.SourceConfigRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final SourceConfigRepository sourceRepo;
    private final PlaylistEntryRepository entryRepo;
    private final FilterRuleRepository filterRepo;

    public HomeController(SourceConfigRepository sourceRepo,
                          PlaylistEntryRepository entryRepo,
                          FilterRuleRepository filterRepo) {
        this.sourceRepo = sourceRepo;
        this.entryRepo = entryRepo;
        this.filterRepo = filterRepo;
    }

    @GetMapping("/")
    public String home(Model model) {
        sourceRepo.findAll().stream().findFirst().ifPresent(config -> {
            model.addAttribute("config", config);
        });
        model.addAttribute("totalEntries", entryRepo.count());
        model.addAttribute("totalCategories", filterRepo.count());
        model.addAttribute("includedCategories",
                filterRepo.findAll().stream().filter(r -> r.isIncluded()).count());
        return "index";
    }
}
