package com.github.robert2411.m3uplaylistfilter.controller;

import com.github.robert2411.m3uplaylistfilter.entity.FilterRule;
import com.github.robert2411.m3uplaylistfilter.repository.FilterRuleRepository;
import com.github.robert2411.m3uplaylistfilter.repository.PlaylistEntryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CategoryController {

    private final FilterRuleRepository filterRepo;
    private final PlaylistEntryRepository entryRepo;

    public CategoryController(FilterRuleRepository filterRepo, PlaylistEntryRepository entryRepo) {
        this.filterRepo = filterRepo;
        this.entryRepo = entryRepo;
    }

    @GetMapping("/categories")
    public String categories(Model model,
                             @RequestParam(required = false) String search,
                             @RequestParam(defaultValue = "name") String sort) {
        List<FilterRule> allRules = filterRepo.findAllByOrderByGroupTitleAsc();

        Map<String, Long> counts = entryRepo.countByGroupTitle().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

        // Build groupTitle → streamType map (a group may have only one dominant type)
        Map<String, String> groupToType = entryRepo.findGroupTitleStreamTypes().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> row[1] != null ? (String) row[1] : "live",
                        (a, b) -> a   // keep first if duplicates
                ));

        List<FilterRule> rules = allRules;
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            rules = rules.stream()
                    .filter(r -> r.getGroupTitle().toLowerCase().contains(lower))
                    .toList();
        }
        if ("count".equals(sort)) {
            rules = rules.stream()
                    .sorted(Comparator.comparingLong((FilterRule r) ->
                            counts.getOrDefault(r.getGroupTitle(), 0L)).reversed())
                    .toList();
        }

        List<FilterRule> liveRules   = rules.stream().filter(r -> "live".equals(groupToType.getOrDefault(r.getGroupTitle(), "live"))).toList();
        List<FilterRule> movieRules  = rules.stream().filter(r -> "movie".equals(groupToType.getOrDefault(r.getGroupTitle(), "live"))).toList();
        List<FilterRule> seriesRules = rules.stream().filter(r -> "series".equals(groupToType.getOrDefault(r.getGroupTitle(), "live"))).toList();

        long includedTotal = allRules.stream()
                .filter(FilterRule::isIncluded)
                .mapToLong(r -> counts.getOrDefault(r.getGroupTitle(), 0L))
                .sum();

        model.addAttribute("liveRules", liveRules);
        model.addAttribute("movieRules", movieRules);
        model.addAttribute("seriesRules", seriesRules);
        model.addAttribute("counts", counts);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("includedTotal", includedTotal);
        return "categories";
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggle(@PathVariable Long id, Model model) {
        FilterRule rule = filterRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown rule: " + id));
        rule.setIncluded(!rule.isIncluded());
        filterRepo.save(rule);
        Map<String, Long> counts = entryRepo.countByGroupTitle().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
        model.addAttribute("rule", rule);
        model.addAttribute("counts", counts);
        return "fragments/category-row :: row";
    }

    @PostMapping("/categories/include-all")
    @Transactional
    public String includeAll(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            filterRepo.updateIncludedByStreamType(true, type);
        } else {
            filterRepo.updateAllIncluded(true);
        }
        return "redirect:/categories";
    }

    @PostMapping("/categories/exclude-empty")
    @Transactional
    public String excludeEmpty() {
        filterRepo.excludeEmptyGroups();
        return "redirect:/categories";
    }

    @PostMapping("/categories/delete-empty")
    @Transactional
    public String deleteEmpty() {
        filterRepo.deleteEmptyGroups();
        return "redirect:/categories";
    }

    @PostMapping("/categories/exclude-all")
    @Transactional
    public String excludeAll(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            filterRepo.updateIncludedByStreamType(false, type);
        } else {
            filterRepo.updateAllIncluded(false);
        }
        return "redirect:/categories";
    }
}
