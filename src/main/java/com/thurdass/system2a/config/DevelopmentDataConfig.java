package com.thurdass.system2a.config;
import com.thurdass.system2a.entity.Classroom; import com.thurdass.system2a.repository.ClassroomRepository; import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
@Configuration public class DevelopmentDataConfig { @Bean CommandLineRunner seedClassroom(ClassroomRepository repo){return args->{if(repo.count()==0)repo.save(new Classroom("2A","Informática",2026,"Manhã"));};} }
