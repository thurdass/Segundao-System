package com.thurdass.system2a.repository;

import com.thurdass.system2a.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByClassroomIdAndActiveTrueOrderByDueDateAsc(Long classroomId);

    long countByActiveTrue();
}
