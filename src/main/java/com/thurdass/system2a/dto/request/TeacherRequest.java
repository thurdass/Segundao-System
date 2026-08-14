package com.thurdass.system2a.dto.request;

import jakarta.validation.constraints.*;

import java.util.*;

public record TeacherRequest(@NotBlank String name, @Email String email, Set<Long> subjectIds) {
}
