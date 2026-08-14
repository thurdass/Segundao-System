package com.thurdass.system2a.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_completion_user_activity", columnNames = {"user_id", "activity_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ActivityCompletion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(optional = false)
    User user;
    @ManyToOne(optional = false)
    Activity activity;
    @Column(nullable = false)
    LocalDateTime completedAt = LocalDateTime.now();
}
