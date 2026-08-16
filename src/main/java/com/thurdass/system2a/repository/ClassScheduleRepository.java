package com.thurdass.system2a.repository;

import com.thurdass.system2a.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    List<ClassSchedule> findByClassroomIdOrderByDayOfWeekAscStartTimeAsc(Long classroomId);

    List<ClassSchedule> findBySubjectIdAndClassroomId(Long subjectId, Long classroomId);
}
