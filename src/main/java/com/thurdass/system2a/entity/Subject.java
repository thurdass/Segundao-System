package com.thurdass.system2a.entity;
import jakarta.persistence.*; import lombok.*; import java.util.*;
@Entity @Getter @Setter @NoArgsConstructor public class Subject { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(nullable=false,length=120) String name; @Column(length=30) String shortName; @ManyToOne(fetch=FetchType.LAZY,optional=false) Classroom classroom; @Column(nullable=false) boolean active=true; @ManyToMany(mappedBy="subjects") Set<Teacher> teachers=new HashSet<>(); public Subject(String n,String s,Classroom c){name=n;shortName=s;classroom=c;} }
