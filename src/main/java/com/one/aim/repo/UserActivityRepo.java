package com.one.aim.repo;

import com.one.aim.bo.UserActivityBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepo extends JpaRepository<UserActivityBO, Long> {

    List<UserActivityBO> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT ua FROM UserActivityBO ua ORDER BY ua.createdAt DESC")
    List<UserActivityBO> findAllOrderByCreatedAtDesc();

    Page<UserActivityBO> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    @Query("""
    SELECT ua.activityType, COUNT(DISTINCT ua.userId) as userCount
    FROM UserActivityBO ua
    WHERE ua.createdAt >= :start AND ua.createdAt < :end
    GROUP BY ua.activityType
    ORDER BY userCount DESC
""")
    List<Object[]> getWeeklyActiveUsersByType(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );


}
