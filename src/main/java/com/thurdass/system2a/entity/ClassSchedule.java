package com.thurdass.system2a.entity;
import jakarta.persistence.*; import lombok.*; import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor public class ClassSchedule { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @ManyToOne(optional=false) Classroom classroom; @ManyToOne(optional=false) Subject subject; @ManyToOne Teacher teacher; @Enumerated(EnumType.STRING) @Column(nullable=false) DayOfWeek dayOfWeek; @Column(nullable=false) LocalTime startTime; @Column(nullable=false) LocalTime endTime; }
