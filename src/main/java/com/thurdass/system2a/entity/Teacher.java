package com.thurdass.system2a.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 120)
    String name;
    @Column(length = 160)
    String email;
    @Column(nullable = false)
    boolean active = true;
    @ManyToMany
    @JoinTable(name = "teacher_subject", joinColumns = @JoinColumn(name = "teacher_id"), inverseJoinColumns = @JoinColumn(name = "subject_id"))
    Set<Subject> subjects = new HashSet<>();

    public Teacher(String teacherName, String teacherEmail) {
        name = teacherName;
        email = teacherEmail;
    }
}
