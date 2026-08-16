package com.thurdass.system2a.config;

import com.thurdass.system2a.entity.Classroom;
import com.thurdass.system2a.repository.ClassroomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DevelopmentDataConfig {
    @Bean
    @Order(1)
    CommandLineRunner seedClassroom(ClassroomRepository classroomRepository) {
        return applicationArguments -> {
            if (classroomRepository.count() == 0) {
                classroomRepository.save(new Classroom("2A", "Informática", 2026, "Manhã"));
            }
        };
    }
}
