package com.thurdass.system2a.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 160)
    String title;
    @Column(nullable = false, length = 4000)
    String content;
    @Column(nullable = false)
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    @ManyToOne(optional = false)
    User createdBy;
    @ManyToOne(optional = false)
    Classroom classroom;
    boolean pinned;
    boolean active = true;

    @PrePersist
    void initializeTimestamps() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
