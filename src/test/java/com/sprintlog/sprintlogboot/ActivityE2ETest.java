package com.sprintlog.sprintlogboot;

import com.jayway.jsonpath.JsonPath;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.repository.AuditLogRepository;
import com.sprintlog.sprintlogboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

// 가짜 흉내가 아니라 진짜 서버를 충동 없는 랜덤 포트 번호로 띄어서 진짜 HTTP 통신 테스트를 진행 하겠다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("활동 E2E 통합 테스트(@SpringBootTest + TestRestTemplate)")
public class ActivityE2ETest {

    @LocalServerPort int port; //서버가 실제로 배정받은 포트 번호를 이 필드에 주입.
    @Autowired
    TestRestTemplate rest; // 진짜 HTTP 요청을 보내는 클라이언트를 주입받습니다.

    @Autowired
    ActivityRepository activityRepository;
    @Autowired
    AuditLogRepository auditLogRepository;
    @Autowired
    UserRepository userRepository;

    private String base; // 공통 기본 url 담아놓을 용도.

    @BeforeEach
    void Clean() {
        activityRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        base = "http://localhost:" + port + "/api/v1/activities";
    }

    //생성 요청을 멀티파트로 보내주는 헬퍼 메서드 입니다. JSON 문자열을 넘기면 알아서 멀티파드로 포장해 준다.
    private ResponseEntity<String> multipartCreate(String dataJson) {
        HttpHeaders dataHeaders = new HttpHeaders();
        dataHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> dataPart = new HttpEntity<>(dataJson, dataHeaders);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        parts.add("data", dataPart);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);



        return rest.postForEntity(base, new HttpEntity<>(parts, headers), String.class);
    }


    // 수정(PUT)은 JSON 본문(@RequestBody) 이라 그대로 보낸다.
    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }


    @Test
    @DisplayName("생성->조회 왕복 POST로 만든 활동을 그 Location으로 다시 GET하면 같은 활동이 온다.")
    void 생성하고_다시_조회() {
        // (1) 진짜 HTTP POST -> 컨트롤러 -> 서비스 -> 진짜 DB에 저장
        ResponseEntity<String> created = multipartCreate("""
                {"category":"LECTURE","title":"E2E 강의","minutes":45,
                 "visibility":"PUBLIC","instructorName":"이강사","studiedOn":"2026-01-01"}
                """);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED); // 201 응답이 왔을 것이다.
        URI location = created.getHeaders().getLocation();
        assertThat(location).isNotNull(); // Location 헤더가 있을 것이다.

        // (2) 그 주소로 다시 GET -> 방금 저장한 게 진짜 DB에서 나올 것이다.
        ResponseEntity<String> fetched = rest.getForEntity(
                "http://localhost:" + port + location.getPath(), String.class
        );

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String)JsonPath.read(fetched.getBody(), "$.title")).isEqualTo("E2E 강의");

    }


    @Test
    @DisplayName("검증 실패 - 빈 제목이면 진짜 HTTP로도 400 + ProblemDetail(code C001)")
    void 검증실패_400() {
        // when
        ResponseEntity<String> res = multipartCreate("""
                {"category":"LECTURE","title":"E2E 강의","minutes":45,
                 "visibility":"PUBLIC","instructorName":"이강사","studiedOn":"2026-01-01"}
                """);
        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String)JsonPath.read(res.getBody(),"$.code")).isEqualTo("C001");

    }


    @Test
    @DisplayName("없는 자원 - 존재하지 않는 id 조회는 404 + ProblemDetail(code A001)")
    void 없으면_404() {
        ResponseEntity<String> res = rest.getForEntity(base + "/99999", String.class);


        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((String)JsonPath.read(res.getBody(),"$.code")).isEqualTo("A001");
    }


    // 전체 생명주기 시나리오 — E2E 의 진짜 값어치. 한 자원이 생성→수정→삭제 를 거치는 사용자 흐름 을
    // 진짜 HTTP 로 통과시키며, 각 단계의 결과가 *진짜 DB 에 이어져* 반영되는지 본다. (조각 테스트로는 못 보는 것)
    @Test
    @DisplayName("생명주기 — 생성→수정→조회→삭제→다시 조회하면 404")
    void 생성_수정_삭제_생명주기() {
        // 1) 생성(POST, 멀티파트) → 201, id 확보
        ResponseEntity<String> created = multipartCreate("""
                {"category":"LECTURE","title":"처음 제목","minutes":30,"visibility":"PUBLIC","instructorName":"이강사"}
                """);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long id = ((Number) JsonPath.read(created.getBody(), "$.id")).longValue();
        String one = base + "/" + id;

        // 2) 수정(PUT, JSON) → 200. postForEntity 처럼 지름길이 없어 exchange(HttpMethod.PUT, ...) 로 보낸다.
        ResponseEntity<String> updated = rest.exchange(one, HttpMethod.PUT,
                json("{\"title\":\"바뀐 제목\",\"visibility\":\"PRIVATE\"}"), String.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) JsonPath.read(updated.getBody(), "$.title")).isEqualTo("바뀐 제목");

        // 3) 조회(GET) → 수정이 진짜 DB 에 반영됐는지 확인
        assertThat((String) JsonPath.read(rest.getForEntity(one, String.class).getBody(), "$.title"))
                .isEqualTo("바뀐 제목");

        // 4) 삭제(DELETE) → 204(본문 없음)
        ResponseEntity<Void> deleted = rest.exchange(one, HttpMethod.DELETE, null, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 5) 다시 조회 → 이제 없다(404). 삭제까지 전 계층으로 이어져 반영됨.
        assertThat(rest.getForEntity(one, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
