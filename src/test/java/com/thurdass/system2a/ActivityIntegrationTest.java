package com.thurdass.system2a;

import com.thurdass.system2a.entity.*;
import com.thurdass.system2a.enums.DeadlineMode;
import com.thurdass.system2a.repository.*;
import com.thurdass.system2a.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.*;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityIntegrationTest {
    private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ClassroomRepository classroomRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    SubjectRepository subjectRepository;
    @Autowired
    ActivityRepository activityRepository;
    @Autowired
    CompletionRepository completionRepository;
    @Autowired
    ClassScheduleRepository scheduleRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;

    @Test
    void createsActivityUsingAuthenticatedCreatorAndOwnClassroom() throws Exception {
        Classroom classroom = classroom("creation");
        User student = student("creator", classroom);
        Subject subject = subject("Web", classroom);
        LocalDate due = tomorrow();

        MvcResult result = mockMvc.perform(post("/api/activities")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("HTML assignment", subject.getId(), "CUSTOM_DATE", due)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("HTML assignment"))
                .andExpect(jsonPath("$.createdBy").value(student.getUsername()))
                .andExpect(jsonPath("$.classroomId").value(classroom.getId()))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn();

        Activity saved = activityRepository.findById(idFrom(result)).orElseThrow();
        assertEquals(student.getId(), saved.getCreatedBy().getId());
        assertEquals(classroom.getId(), saved.getClassroom().getId());
        assertEquals(due, saved.getDueDate());
    }

    @Test
    void rejectsOtherClassroomSubjectAndInvalidRequest() throws Exception {
        Classroom own = classroom("own");
        Classroom other = classroom("other");
        User student = student("cross-class", own);
        Subject otherSubject = subject("Other subject", other);

        mockMvc.perform(post("/api/activities")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("Invalid class", otherSubject.getId(), "CUSTOM_DATE", tomorrow())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Activity must belong to your classroom"));

        mockMvc.perform(post("/api/activities")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\" \",\"subjectId\":" + otherSubject.getId() + ",\"deadlineMode\":\"CUSTOM_DATE\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/activities")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("Missing subject", 999999L, "CUSTOM_DATE", tomorrow())))
                .andExpect(status().isNotFound());
    }

    @Test
    void calculatesNextClassDeadlineWhenRequested() throws Exception {
        Classroom classroom = classroom("next-class");
        User student = student("next-class-user", classroom);
        Subject subject = subject("Database", classroom);
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        ClassSchedule schedule = new ClassSchedule();
        schedule.setClassroom(classroom);
        schedule.setSubject(subject);
        schedule.setDayOfWeek(today.plusDays(1).getDayOfWeek());
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(8, 50));
        scheduleRepository.save(schedule);

        MvcResult result = mockMvc.perform(post("/api/activities")
                        .header("Authorization", bearer(student))
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("Next class work", subject.getId(), "NEXT_CLASS", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value(today.plusDays(1).toString()))
                .andReturn();

        assertEquals(today.plusDays(1), activityRepository.findById(idFrom(result)).orElseThrow().getDueDate());
    }

    @Test
    void listsOnlyOwnClassroomAndAppliesSubjectAndDueDateFilters() throws Exception {
        Classroom own = classroom("listing");
        Classroom other = classroom("hidden");
        User student = student("listing-user", own);
        Subject wanted = subject("Wanted", own);
        Subject ignored = subject("Ignored", own);
        Activity visible = activity("Visible", wanted, own, student, tomorrow());
        activity("Wrong subject", ignored, own, student, tomorrow());
        activity("Other class", subject("Foreign", other), other, student("hidden-owner", other), tomorrow());

        mockMvc.perform(get("/api/activities")
                        .header("Authorization", bearer(student))
                        .param("subjectId", wanted.getId().toString())
                        .param("dueBefore", tomorrow().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(visible.getId()))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/activities/" + visible.getId())
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk());
    }

    @Test
    void cannotReadActivityFromAnotherClassroom() throws Exception {
        Classroom first = classroom("read-first");
        Classroom second = classroom("read-second");
        User firstUser = student("reader-first", first);
        User secondUser = student("reader-second", second);
        Activity hidden = activity("Hidden", subject("Hidden subject", second), second, secondUser, tomorrow());

        mockMvc.perform(get("/api/activities/" + hidden.getId())
                        .header("Authorization", bearer(firstUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatorCanEditAndDeleteButAnotherStudentCannot() throws Exception {
        Classroom classroom = classroom("permissions");
        User creator = student("activity-owner", classroom);
        User other = student("activity-other", classroom);
        Subject subject = subject("Permissions", classroom);
        Activity activity = activity("Original", subject, classroom, creator, tomorrow());

        mockMvc.perform(put("/api/activities/" + activity.getId())
                        .header("Authorization", bearer(other))
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("Not allowed", subject.getId(), "CUSTOM_DATE", tomorrow())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/activities/" + activity.getId())
                        .header("Authorization", bearer(creator))
                        .contentType(APPLICATION_JSON)
                        .content(activityJson("Edited", subject.getId(), "CUSTOM_DATE", tomorrow())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited"));

        mockMvc.perform(delete("/api/activities/" + activity.getId())
                        .header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/activities/" + activity.getId())
                        .header("Authorization", bearer(creator)))
                .andExpect(status().isOk());
        assertFalse(activityRepository.findById(activity.getId()).orElseThrow().isActive());
    }

    @Test
    void completionIsIndividualAndDuplicateOrMissingUndoAreRejected() throws Exception {
        Classroom classroom = classroom("completion");
        User first = student("completion-first", classroom);
        User second = student("completion-second", classroom);
        Subject subject = subject("Completion", classroom);
        Activity activity = activity("Complete me", subject, classroom, first, tomorrow());

        mockMvc.perform(post("/api/activities/" + activity.getId() + "/complete")
                        .header("Authorization", bearer(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").exists());

        assertTrue(completionRepository.findByUserIdAndActivityId(first.getId(), activity.getId()).isPresent());

        mockMvc.perform(post("/api/activities/" + activity.getId() + "/complete")
                        .header("Authorization", bearer(first)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/activities/" + activity.getId())
                        .header("Authorization", bearer(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));

        mockMvc.perform(get("/api/activities")
                        .header("Authorization", bearer(second))
                        .param("status", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(activity.getId()));

        mockMvc.perform(get("/api/activities")
                        .header("Authorization", bearer(first))
                        .param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(activity.getId()));

        mockMvc.perform(delete("/api/activities/" + activity.getId() + "/complete")
                        .header("Authorization", bearer(first)))
                .andExpect(status().isOk());
        assertTrue(completionRepository.findByUserIdAndActivityId(first.getId(), activity.getId()).isEmpty());

        mockMvc.perform(delete("/api/activities/" + activity.getId() + "/complete")
                        .header("Authorization", bearer(first)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotCompleteActivityFromAnotherClassroom() throws Exception {
        Classroom own = classroom("completion-own");
        Classroom other = classroom("completion-other");
        User student = student("completion-cross", own);
        User owner = student("completion-owner", other);
        Activity activity = activity("Foreign activity", subject("Foreign completion", other), other, owner, tomorrow());

        mockMvc.perform(post("/api/activities/" + activity.getId() + "/complete")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isNotFound());
    }

    private Classroom classroom(String suffix) {
        return classroomRepository.save(new Classroom("Class " + suffix + unique(), "Informática", 2026, "Manhã"));
    }

    private User student(String suffix, Classroom classroom) {
        User user = new User(unique(suffix), passwordEncoder.encode("password123"), suffix, classroom);
        return userRepository.save(user);
    }

    private Subject subject(String name, Classroom classroom) {
        return subjectRepository.save(new Subject(name + unique(), "SUB", classroom));
    }

    private Activity activity(String title, Subject subject, Classroom classroom, User creator, LocalDate dueDate) {
        Activity activity = new Activity();
        activity.setTitle(title + unique());
        activity.setDescription("Description");
        activity.setDueDate(dueDate);
        activity.setSubject(subject);
        activity.setClassroom(classroom);
        activity.setCreatedBy(creator);
        return activityRepository.save(activity);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generate(user);
    }

    private long idFrom(MvcResult result) throws Exception {
        Matcher matcher = ID.matcher(result.getResponse().getContentAsString());
        assertTrue(matcher.find(), "Response must contain an activity id");
        return Long.parseLong(matcher.group(1));
    }

    private LocalDate tomorrow() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);
    }

    private String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private String unique(String prefix) {
        return prefix + unique();
    }

    private String activityJson(String title, Long subjectId, String mode, LocalDate dueDate) {
        String due = dueDate == null ? "null" : "\"" + dueDate + "\"";
        return "{\"title\":\"" + title + "\",\"description\":\"Test description\",\"subjectId\":" + subjectId + ",\"deadlineMode\":\"" + mode + "\",\"dueDate\":" + due + "}";
    }
}
