package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Native;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest // JPA 계층 관련 빈만 로딩(Service, Controller, Component는 로딩하지 않음)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class ActivityRepositoryTest {

    @Autowired // 테스트 환경에서는 생정자 의존성 주입을 사용할 수 없어서 @Autowired로 직접 주입해 주세요.
    ActivityRepository activityRepository;

    @Autowired
    TestEntityManager em;

    @Autowired
    EntityManagerFactory emf;

    @BeforeEach
    void setUp() {
        persist(ActivityCategory.LECTURE,  "Spring Boot 입문", 90,  Visibility.PUBLIC,  "spring", "java");
        persist(ActivityCategory.LECTURE,  "JPA 심화",         120, Visibility.PUBLIC,  "spring", "jpa");
        persist(ActivityCategory.READING,  "클린 코드",         60,  Visibility.PRIVATE, "clean");
        persist(ActivityCategory.PRACTICE, "알고리즘 연습",     45,  Visibility.PUBLIC);
        // 더미 데이터 세팅 후에 진행될 테스트를 좀 더 깔끔하게 진행하기 위해서 TestEntityManager로 영속성 컨텍스트를 직접 제어
        em.flush(); // 영속성 컨텍스트에 영속된 엔터티들을 강제로 밀어내기 -> INSERT
        em.clear(); // 영속성 컨텍스트 비우기
    }

    private void persist(ActivityCategory category, String title, int minutes, Visibility visibility, String... tags) {
        LearningActivity activity = new LearningActivity(category, title, minutes, visibility, null, null, null);
        for (String tag : tags) {
            activity.addTag(tag);
        }
        em.persist(activity);
    }

    // 지금까지 하이버네이트가 실행한 SQL 문의 개수를 리턴.
    private long queryCount(){
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        return stats.getPrepareStatementCount();
    }


    @Nested
    @DisplayName("직접 작성한 @Query")
    class CustomQueries{
        @Test
        @DisplayName("findByTag - 특정 태그를 가진 활동만 (컬렉션 조인)")
        void findByTag() {
            // when
            List<LearningActivity> result = activityRepository.findByTag("spring");

            // then
            assertThat(result)
                    .extracting(LearningActivity::getTitle)
                    .containsExactlyInAnyOrder("Spring Boot 입문", "JPA 심화");

            // 조회된 객체들이 해당 태그를 직접 가지고 있는지를 단언
            assertThat(result)
                    .isNotEmpty()
                    .allMatch(a -> a.hasTag("spring"));
        }

        @Test
        @DisplayName("findByTag - 없는 태그면 빈 결과")
        void findByTag_없으면_빈결과() {
            assertThat(activityRepository.findByTag("존재하지 않는태그")).isEmpty();
        }

        @Test
        @DisplayName("findLongActivities - 기준 분 이상, 시간 내림차순")
        void findLongActivities() {
            List<LearningActivity> result = activityRepository.findLongActivities(90);

            assertThat(result)
                    .extracting(LearningActivity::getMinutes)
                    .containsExactly(120, 90);

        }

        @Test
        @DisplayName("findLongActivitesNative - 네이티브 SQL도 엔터티로 정확히 매핑(JPQL과 같은 결과)")
        void nativeQuery_JPQL과_동일() {
            List<LearningActivity> jpql = activityRepository.findLongActivities(90);
            List<LearningActivity> nativeR = activityRepository.findLongActivitiesNative(90);


            assertThat(nativeR).extracting(LearningActivity::getMinutes)
                    .containsExactly(120, 90);

            assertThat(nativeR).extracting(LearningActivity::getTitle)
                    .containsExactlyElementsOf(jpql.stream().map(LearningActivity::getTitle).toList());
        }
    }

    @Nested
    @DisplayName("복합 조건 메서드명 쿼리")
    class DerivedQueries {
        @Test
        @DisplayName("findByCategoryAndVisibilityOrderByMinutesDesc - 두 조건 AND + 정렬")
        void categoryAndVisiblityOrdered() {
            // when
            List<LearningActivity> result =
                    activityRepository.findByCategoryAndVisibilityOrderByMinutesDesc(ActivityCategory.LECTURE, Visibility.PUBLIC);
            // then
            assertThat(result).extracting(LearningActivity::getTitle)
                    .containsExactly("JPA 심화", "Spring Boot 입문");
        }

        @Test
        @DisplayName("findByTitleContainingIgnoreCase - 부분 일치 + 대소문자 무시")
        void titleContainingIgnoreCase() {
            // when
            List<LearningActivity> result = activityRepository.findByTitleContainingIgnoreCase("jpa 심화");

            // then
            assertThat(result).extracting(LearningActivity::getTitle)
                    .containsExactly("JPA 심화");

        }
    }
    @Nested
    @DisplayName("N+1 회피 (fetch 전략) - 쿼리 '수'를 검증")
    class FetchStrategy {
        @Test
        @DisplayName("findAll - 연관(태그)을 활동마다 또 조회 = N+1(1 + N = 4 쿼리)")
        void findAll_은_N플러스1() {
            // given
            long before = queryCount();

            // when
            List<LearningActivity> all = activityRepository.findAll();
            all.forEach(a -> a.getTags().size()); // 활동 객체에서 태그를 꺼내쓴다 -> LAZY면 여기서 활동마다 N+1 조회가 발생
            long executed = queryCount() - before;

            // then
            assertThat(all).hasSize(4);
            assertThat(executed).isEqualTo(5);
        }

        @Test
        @DisplayName("findAllWithDetails - @EntityGraph")
        void withDetails_는_한_쿼리() {
            // given
            long before = queryCount();

            // when
            List<LearningActivity> all = activityRepository.findAllWithDetails();
            all.forEach(a -> a.getTags().size());
            long executed = queryCount() - before;

            // then
            assertThat(all).hasSize(4);
            assertThat(executed).isEqualTo(1);
        }
    }






    /*
        # 메서드 이름 작성 관례
        1. Snake Case
        가장 전통적이고 가독성이 좋아 흔하게 사용하는 방식
        형식: 테스트대상_테스트조건_예상결과
        ex): void signUp_invalidEmail_throwException() { ... }
             void findById_exists_returnMember() { ... }

        2. BDD 스타일
        행위 주도 개발의 영향을 받은 스타일로, 외국에서 많이 사용하는 방식 (should - when)
        형식: should_예상행위_when_테스트조건
        ex): should_throwException_when_EmailIsInvalid() { ... }

        3. 한글 메서드 이름
        가독성을 최우선으로 하여 한글로 메서드 이름을 짓습니다.
        형식: 테스트내용_한글로서술
        ex): void 회원가입_실패_중복된_이메일() { ... }
             void 주문_성공_재고_차감_확인() { ... }

         결론: 협업 규칙이 1순위 입니다. -> 본인이 소속된 회사의 컨벤션을 따르는 것이 법입니다.
         메서드 이름을 고민하느라 시간을 소요하지 마세요. @DisplayName이 있으니까요.
         Given-When-Then 패턴을 잘 지켜주시고, 주석은 꼭 남겨 놓으세요.
         */
}
