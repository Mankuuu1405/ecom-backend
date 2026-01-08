package com.one.aim.repo;

import com.one.aim.bo.NotificationUserStatusBO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationUserStatusRepo extends JpaRepository<NotificationUserStatusBO, Long> {

    List<NotificationUserStatusBO> findByUserIdAndIsHiddenFalseOrderByCreatedAtDesc(Long userId);

    List<NotificationUserStatusBO> findByUserIdAndIsReadFalseAndIsHiddenFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalseAndIsHiddenFalse(Long userId);

    @Query("""
SELECT s FROM NotificationUserStatusBO s
WHERE s.userId = :userId
AND s.isHidden = false
AND (:unread IS NULL OR s.isRead = false)
AND (:type IS NULL OR s.event.type = :type)
ORDER BY s.createdAt DESC
""")
    Page<NotificationUserStatusBO> findFiltered(
            @Param("userId") Long userId,
            @Param("unread") Boolean unread,
            @Param("type") String type,
            Pageable pageable
    );



}
