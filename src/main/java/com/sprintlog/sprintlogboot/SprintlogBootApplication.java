package com.sprintlog.sprintlogboot;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// BaseEntity의 @CreatedDate, @LastModifiedDate 자동 채움 기능을 켠다.
// 이거 없으면 null로 들dj감
@SpringBootApplication
public class SprintlogBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintlogBootApplication.class, args);
    }
}
