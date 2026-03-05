package com.github.robert2411.m3uplaylistfilter.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "playlist_entry", indexes = @Index(columnList = "group_title"))
public class PlaylistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String name;

    @Column(length = 300)
    private String tvgId;

    @Column(length = 1000)
    private String tvgLogo;

    @Column(length = 300)
    private String groupTitle;

    @Column(length = 2000)
    private String streamUrl;

    @Column(length = 20)
    private String streamType = "live";

    @Column(length = 10)
    private String containerExtension;

    @Column(length = 20)
    private String rating;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTvgId() { return tvgId; }
    public void setTvgId(String tvgId) { this.tvgId = tvgId; }

    public String getTvgLogo() { return tvgLogo; }
    public void setTvgLogo(String tvgLogo) { this.tvgLogo = tvgLogo; }

    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    public String getStreamType() { return streamType; }
    public void setStreamType(String streamType) { this.streamType = streamType; }

    public String getContainerExtension() { return containerExtension; }
    public void setContainerExtension(String containerExtension) { this.containerExtension = containerExtension; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
}
