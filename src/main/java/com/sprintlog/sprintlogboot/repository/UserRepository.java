package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring-data-jpa는 인터페이스만 선언해 놓으면 구현체는 자동으로 만들어줌
// JpaRepository 인터페잇를 상속만 받으면 구현체가 알아서 셋팅된다.
// 제네릭에는 <엔터티 타입, PK 타입>
public interface UserRepository extends JpaRepository<User, Long> {
}
