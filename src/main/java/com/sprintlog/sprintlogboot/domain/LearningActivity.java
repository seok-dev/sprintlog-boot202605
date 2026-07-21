package com.sprintlog.sprintlogboot.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sprintlog.sprintlogboot.exception.InvalidActivityException;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "activities")
public class LearningActivity extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int minutes;

    //enum 타입을 DB에 어떻게 넣을지 정의 (STRING: 상수를 문자열로 변환, ORDINAL: 상수의 순서 숫자를 변환)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private  ActivityCategory category;

    @Column(length = 50) //LECTURE 전용
    private String instructorName;

    private Integer completionRate; //PRACTICE 전용

    @Column(length = 200) // READING 전용
    private String bookTitle;

    @Column(length = 100)
    private String attachmentFileName; //첨부파일(UUID), 필수가 아니기 때문에 null을 허용

    // 컬렉션 자료형을 별도의 테이블로 매핑, 테이블 이름은 activity_tags
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "activity_tags", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "tag")
    private  Set<String> tags = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;


    //JPA가 사용하는 생성자를 protected로 선언 (없으면 JPA가 조회한 내용을 객체로 변환 X)
    protected LearningActivity() {}


    public LearningActivity(ActivityCategory category, String title, int minutes, Visibility visibility,
                            String instructorName, Integer completionRate, String bookTitle) {
        validateTitle(title);
        validateMinutes(minutes);
        this.category = category;
        this.title = title.trim(); // 좌우 공백 제거
        this.minutes = minutes;
        this.visibility = visibility;
        this.instructorName = normalizeInstructorName(category, instructorName);
        this.completionRate = normalizeCompletionRate(completionRate);
        this.bookTitle = bookTitle;
    }

    // 활동의 주인을 지정하는 setter 메서드
    public void assignOwner(User owner) {
        this.owner = owner;
    }

    // 첨부 파일명을 활동 객체에 추가한다. (평범한 setter)
    // DB에는 파일명만, 실제 파일은 디스크에 저장
    public void attachFile(String savedFileName) {
        this.attachmentFileName = savedFileName;
    }



    /**
     * 태그를 추가한다. 공백은 제거하고, 소문자로 저장한다.
     * 중복 태그는 무시한다 (Set의 특성)
     */
    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new InvalidActivityException("태그는 비워둘 수 없습니다.");
        }
        tags.add(tag.trim().toLowerCase());
    }

    public boolean removeTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        return tags.remove(tag.trim().toLowerCase());
    }


    /**
     * 등록된 태그 목록을 읽기 전용으로 반환한다.
     */
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * 해당 태그가 등록되어 있는지 확인한다.
     */
    public boolean hasTag(String tag) {
        if (tag == null) return false;
        return tags.contains(tag.trim().toLowerCase());
    }



//    public static int getTotalCreatedCount() {
//        return totalCreateCount;
//    }

    public void extendStudy(int additionalMinutes) {
        if (additionalMinutes <= 0) {
            throw new InvalidActivityException(
                    "추가 학습 시간은 1분 이상이어야 합니다. 입력값: " + additionalMinutes);
        }

        this.minutes += additionalMinutes;
    }

    public void changeTitle(String newTitle) {
        validateTitle(newTitle);
        this.title = newTitle;
    }

    private void validateTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new InvalidActivityException("학습 제목은 비워둘 수 없습니다.");
        }
    }

    private void validateMinutes(int newMinutes) {
        if (newMinutes <= 0) {
            throw new InvalidActivityException("학습 시간은 1분 이상이여야 합니다. 입력값: " + newMinutes);
        }
    }

    public void openToPublic() {
        this.visibility = Visibility.PUBLIC;
    }

    public void hideFromPublic() {
        this.visibility = Visibility.PRIVATE;
    }

    // 이전 LectureLog 의 정규화 로직을 흡수: 강의인데 강사명이 비면 "강사 미정".
    private static String normalizeInstructorName(ActivityCategory category, String instructorName) {
        if (category == ActivityCategory.LECTURE && (instructorName == null || instructorName.isBlank())) {
            return "강사 미정";
        }
        return instructorName;
    }

    // 이전 PracticeLog 의 정규화 로직을 흡수: 완료율은 0~100 범위로 보정(없으면 null 유지).
    private static Integer normalizeCompletionRate(Integer completionRate) {
        if (completionRate == null) {
            return null;
        }
        if (completionRate < 0) {
            return 0;
        }
        if (completionRate > 100) {
            return 100;
        }
        return completionRate;
    }
}
