package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

//모든 엔터티가 공통으로 가지는것 - 식별자(id)와 생성, 수정 시각 - 을 한곳에 모은 상위 클래스
@Getter
@MappedSuperclass // 이 클래스 자체는 테이블이 되지 않는다. 대신 이 클래스를 상속한 엔터티의 테이블에 여기 선언된 컬럼이 합쳐져서 들어간다.
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 숫자 자동 증가 전략 사용
    private Long id;

    // 생성 시간 - 처음 처장될 때 한번 채워지고, 이후 바뀌지 않는다.
    @CreatedDate // 스프링에서 제공하는 어노테이션
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 수정 시간 - 저장될 때마다 현재 시각으로 갱신
    @LastModifiedDate // 스프링에서 제공하는 어노테이션
    private LocalDateTime updatedAt;

}
