package com.github.robert2411.m3uplaylistfilter.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "source_config")
public class SourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String sourceUrl;

    private LocalDateTime lastImported;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public LocalDateTime getLastImported() { return lastImported; }
    public void setLastImported(LocalDateTime lastImported) { this.lastImported = lastImported; }
}
