package com.thurdass.system2a.repository;

import com.thurdass.system2a.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CompletionRepository extends JpaRepository<ActivityCompletion, Long> {
    Optional<ActivityCompletion> findByUserIdAndActivityId(Long userId, Long activityId);

    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
}
