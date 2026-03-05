package com.github.robert2411.m3uplaylistfilter.repository;

import com.github.robert2411.m3uplaylistfilter.entity.FilterRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FilterRuleRepository extends JpaRepository<FilterRule, Long> {

    Optional<FilterRule> findByGroupTitle(String groupTitle);

    List<FilterRule> findAllByOrderByGroupTitleAsc();

    @Modifying
    @Query("UPDATE FilterRule f SET f.included = :included")
    int updateAllIncluded(@Param("included") boolean included);

    @Modifying
    @Query("UPDATE FilterRule f SET f.included = :included WHERE f.groupTitle IN (SELECT DISTINCT e.groupTitle FROM PlaylistEntry e WHERE e.streamType = :streamType)")
    int updateIncludedByStreamType(@Param("included") boolean included, @Param("streamType") String streamType);

    @Modifying
    @Query("UPDATE FilterRule f SET f.included = false WHERE NOT EXISTS (SELECT e FROM PlaylistEntry e WHERE e.groupTitle = f.groupTitle)")
    int excludeEmptyGroups();

    @Modifying
    @Query("DELETE FROM FilterRule f WHERE NOT EXISTS (SELECT e FROM PlaylistEntry e WHERE e.groupTitle = f.groupTitle)")
    int deleteEmptyGroups();
}
