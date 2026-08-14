package com.thurdass.system2a.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Classroom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, length=120) private String course;
    @Column(nullable=false) private Integer schoolYear;
    @Column(length=30) private String shift;
    public Classroom(String name, String course, Integer schoolYear, String shift) { this.name=name; this.course=course; this.schoolYear=schoolYear; this.shift=shift; }
}
