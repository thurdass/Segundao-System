package com.thurdass.system2a.dto.request; import jakarta.validation.constraints.*; public record SubjectRequest(@NotBlank String name,String shortName,@NotNull Long classroomId){}
