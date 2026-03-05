package com.github.robert2411.m3uplaylistfilter.repository;

import com.github.robert2411.m3uplaylistfilter.entity.FilterRule;
import com.github.robert2411.m3uplaylistfilter.entity.PlaylistEntry;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.stream.Stream;

public interface PlaylistEntryRepository extends JpaRepository<PlaylistEntry, Long> {

    @Query("SELECT e.groupTitle, COUNT(e) FROM PlaylistEntry e GROUP BY e.groupTitle")
    List<Object[]> countByGroupTitle();

    @Query("SELECT e.groupTitle, e.streamType FROM PlaylistEntry e GROUP BY e.groupTitle, e.streamType")
    List<Object[]> findGroupTitleStreamTypes();

    @Query("SELECT e FROM PlaylistEntry e JOIN FilterRule f ON e.groupTitle = f.groupTitle WHERE f.included = true")
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "500"))
    Stream<PlaylistEntry> streamIncluded();

    @Query("SELECT e FROM PlaylistEntry e JOIN FilterRule f ON e.groupTitle = f.groupTitle WHERE f.included = true")
    List<PlaylistEntry> findAllIncluded();

    @Query("SELECT e FROM PlaylistEntry e WHERE e.groupTitle = :groupTitle AND e.streamUrl IS NOT NULL AND e.streamUrl <> ''")
    List<PlaylistEntry> findByGroupTitle(@Param("groupTitle") String groupTitle);

    @Query("SELECT e FROM PlaylistEntry e JOIN FilterRule f ON e.groupTitle = f.groupTitle WHERE f.included = true AND e.streamType = :streamType AND e.streamUrl IS NOT NULL AND e.streamUrl <> ''")
    List<PlaylistEntry> findAllIncludedByType(@Param("streamType") String streamType);

    @Query("SELECT e FROM PlaylistEntry e WHERE e.groupTitle = :groupTitle AND e.streamType = :streamType AND e.streamUrl IS NOT NULL AND e.streamUrl <> ''")
    List<PlaylistEntry> findByGroupTitleAndType(@Param("groupTitle") String groupTitle, @Param("streamType") String streamType);
}
