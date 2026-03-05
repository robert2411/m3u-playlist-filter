package com.github.robert2411.m3uplaylistfilter.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "filter_rule")
public class FilterRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 300)
    private String groupTitle;

    private boolean included;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }

    public boolean isIncluded() { return included; }
    public void setIncluded(boolean included) { this.included = included; }
}
