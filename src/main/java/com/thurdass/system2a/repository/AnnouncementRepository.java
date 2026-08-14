package com.thurdass.system2a.repository;

import com.thurdass.system2a.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByClassroomIdAndActiveTrueOrderByPinnedDescCreatedAtDesc(Long id);

    long countByActiveTrue();
}
