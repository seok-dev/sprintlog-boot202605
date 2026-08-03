package com.sprintlog.sprintlogboot.service;

import com.sprintlog.sprintlogboot.domain.ActivityCategory;
import com.sprintlog.sprintlogboot.domain.LearningActivity;
import com.sprintlog.sprintlogboot.domain.Visibility;
import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import com.sprintlog.sprintlogboot.dto.request.UpdateActivityRequest;
import com.sprintlog.sprintlogboot.exception.ActivityNotFoundException;
import com.sprintlog.sprintlogboot.repository.ActivityRepository;
import com.sprintlog.sprintlogboot.repository.AuditLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityService 슬라이스 테스트 (Mockito)")
class ActivityServiceTest {

    // ActivityService의 기능을 테스트 하는 것이기에, 나머지 의존객체는 가짜로 채워 넣는다.
    @Mock ActivityRepository repository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock AuditService auditService;

    // 서비스의 create는 timer가 걸려 있음 -> MeterRegistry의 timer는 실제로 동작해야 합니다.  (Mock 안됨!)
    // @Spy를 걸어서 실제 기능이 동작할 수 있는 객체로 둔다.
    @Spy MeterRegistry meterRegistry = new SimpleMeterRegistry();


    @InjectMocks
    ActivityService service; // 가짜 객체들을 주입받은 Service 객체를 사용힌다.

    private LearningActivity sample() {
        return new LearningActivity(ActivityCategory.LECTURE, "옛 제목", 30, Visibility.PRIVATE, "이강사", null, null);
    }

    @Nested
    @DisplayName("조회(Get)")
    class Get {
        @Test
        @DisplayName("있으면 그 활동을 돌려준다.")
        void 있으면_반환() {
            // given
            LearningActivity activity = sample();
            when(repository.findById(1L)).thenReturn(Optional.of(activity));

            // when // then
            assertThat(service.get(1L)).isSameAs(activity);


        }

        @Test
        @DisplayName("없으면 ActivityNotFoundException 발생")
        void 없으면_예외() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when // then
            assertThatThrownBy(() -> service.get(999L)).isInstanceOf(ActivityNotFoundException.class);

        }
    }
    
    @Nested
    @DisplayName("수정(update)")
    class Update {
        @Test
        @DisplayName("제목과 공개여부를 바꾸고 저장한다.")
        void 정상_수정() {
            // given
            LearningActivity activity = sample();
//            when(repository.findById(999L)).thenReturn(Optional.empty()); 기본 Mockito 방식
            given(repository.findById(1L)).willReturn(Optional.of(activity)); // BDD 방식의 Mockito 방식 (최근 선호)
            given(repository.save(any(LearningActivity.class))).willReturn(activity);
            // when
            service.update(1L, new UpdateActivityRequest("새 제목", Visibility.PUBLIC));
        
            // then
            assertThat(activity.getTitle()).isEqualTo("새 제목");
            assertThat(activity.getVisibility()).isEqualTo(Visibility.PUBLIC);
            verify(repository).save(activity);
        }
        
        @Test
        @DisplayName("대상이 없다면 예외 - 저장은 일어나지 않는다.")
        void 없으면_변경없음() {
            // given
            given(repository.findById(99L)).willReturn(Optional.empty());

            // when // then
            assertThatThrownBy(()-> service.update(999L, new UpdateActivityRequest("x", Visibility.PUBLIC)))
                    .isInstanceOf(ActivityNotFoundException.class);
            verify(repository, never()).save(any());
        }
        
        
        @Nested
        @DisplayName("살제(delete)")
        class Delete {
            @Test
            @DisplayName("존재하면 그 id로 삭제한다. (existsByid 확인)")
            void 저장_삭제() {
                // given
                given(repository.existsById(1L)).willReturn(true);

                // when // then
                service.delete(1L);

                // then
                verify(repository).deleteById(1L);
            }

            @Test
            @DisplayName("없으면 예외 - 삭제는 일어나지 않는다.")
            void 없으면_삭제안함() {
                // given
                given(repository.existsById(999L)).willReturn(false);

                // when // then
                assertThatThrownBy(() -> service.delete(999L))
                        .isInstanceOf(ActivityNotFoundException.class);
                verify(repository, never()).deleteById(any());
            }
        }

        @Nested
        @DisplayName("저장된 '내용' 검증 - ArgumentCaptor")
        class SavedContent {
            @Test
            @DisplayName("수정 시 저장되는 엔티티의 제목은 요청대로 바뀌어 있다.")
            void 저장내용_검증() {

                // given
                LearningActivity activity = sample();
                given(repository.findById(1L)).willReturn(Optional.of(activity));
                given(repository.save(any(LearningActivity.class))).willReturn(activity);

                // when
                service.update(1L, new UpdateActivityRequest("바뀐 제목", Visibility.PUBLIC));
                ArgumentCaptor<LearningActivity> captor = forClass(LearningActivity.class);

                // then
                verify(repository).save(captor.capture());
                assertThat(captor.getValue().getTitle()).isEqualTo("바뀐 제목");
            }
        }


        @Nested
        @DisplayName("생성(create) - 내부에서 만든 엔터티를 ArqumentCaptor로 검증")
        class Create{
            @Test
            @DisplayName("요청을 매핑에 저장한다 - 저장되는 엔터티의 필드가 요청과 일치")
            void 요청을_매핑해_저장한다 () {
                // given
                CreateActivityRequest request = new CreateActivityRequest(
                        ActivityCategory.LECTURE, "새 강의", 45, Visibility.PUBLIC,
                        null, null, "이강사", null, null);

                // willReturn은 리턴할 객체를 직접 명시해 주어야 합니다.
                // create()를 테스트 할 때는 이 테스트 메서드 안에 리턴해줄 Entity가 없어요
                given(repository.save(any(LearningActivity.class)))
                        .willAnswer(inv -> inv.getArgument(0));

                // when
                service.create(request, null);
                ArgumentCaptor<LearningActivity> captor = forClass(LearningActivity.class);

                // then
                verify(repository).save(captor.capture());
                LearningActivity saved = captor.getValue();
                assertThat(saved.getTitle()).isEqualTo("새 강의");
                assertThat(saved.getCategory()).isEqualTo(ActivityCategory.LECTURE);
                assertThat(saved.getMinutes()).isEqualTo(45);
                assertThat(saved.getInstructorName()).isEqualTo("이강사");
            }
        }

        @Nested
        @DisplayName("void 렵력자 다루기 - willThrow")
        class voidCollabolator{
            @Test
            @DisplayName("삭제 협력자가 실패하면 예외가 전파된다.")
            void 삭제_실패_전파() {
                // given
                given(repository.existsById(1L)).willReturn(true);
                // deleteById를 호출하면서 1L을 주면 예외를 일부러 발생 시킨다.
                willThrow(new RuntimeException("DB 오류")).given(repository).deleteById(1L);


                // when & then
                assertThatThrownBy(()-> service.delete(1L))
                        .isInstanceOf(RuntimeException.class);
            }
        }
    }
}