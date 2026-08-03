package com.sprintlog.sprintlogboot.controller;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.service.ActivityDashboard;
import com.sprintlog.sprintlogboot.service.ActivityService;
import com.sprintlog.sprintlogboot.service.FileService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
@DisplayName("ActivityController 웹 계층 테스트")
class ActivityControllerTest {

    @Autowired
    MockMvc mvc; // 진짜 서버를 띄우지 않고도 HTTP 요청과 응답을 가짜로 시뮬레이션 해 주는 스프링의 도구

    // 의존 관계가 있는 객체들은 모두 가짜로 채우자 - 웹 계층만 집중.
    @MockitoBean
    ActivityService service;

    @MockitoBean
    ActivityDashboard dashboard;

    @MockitoBean
    FileService fileService;

    private LearningActivity sample;

    @BeforeEach
    void setUp() {
        sample = new LearningActivity(
                ActivityCategory.LECTURE, "스프링 강의", 30, Visibility.PUBLIC, "이강사", null, null);
        // id 는 원래 DB 가 부여하지만, 여기선 서비스가 가짜라 직접 심어 응답 JSON 의 id 를 확인할 수 있게 한다.
        ReflectionTestUtils.setField(sample, "id", 1L);
    }


    @Nested
    @DisplayName("GET /{id}")
    class GetById {
        @Test
        @DisplayName("존재하면 200 + JOSN 본문(id, title)")
        void 존재하면_200 () throws Exception {
            //given
            given(service.get(1L)).willReturn(sample);


            mvc.perform(get("/api/v1/activities/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.is").value(1))
                    .andExpect(jsonPath("$.title").value("스프링 강의"))
                    .andExpect(jsonPath("$._links.self").exists());
        }

        @Test
        @DisplayName("없으면 예외 → 404 + ProblemDetail(code A001)")
        void 없으면_404() throws Exception {
            given(service.get(999L)).willThrow(new ActivityNotFoundException(999L));

            mvc.perform(get("/api/v1/activities/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("A001"));   // ErrorCode 가 실린다
        }
    }

    @Nested
    @DisplayName("Post(생성) - multipart(data + 선택 file)")
    class Create {
        @Test
        @DisplayName("data 만 보내도 201 + Location (file 은 선택)")
        void 정상이면_201() throws Exception {
            given(service.create(any(), any())).willReturn(sample);

            MockMultipartFile data = new MockMultipartFile("data", "data.json",
                    MediaType.APPLICATION_JSON_VALUE,
                    """
                    {"category":"LECTURE","title":"스프링 강의","minutes":30,"visibility":"PUBLIC","instructorName":"이강사"}
                    """.getBytes());

            mvc.perform(multipart("/api/v1/activities").file(data))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/activities/1"))
                    .andExpect(jsonPath("$.title").value("스프링 강의"));

            verify(fileService, never()).saveFile(any());   // 파일이 없으면 저장도 안 함
        }
    }























}