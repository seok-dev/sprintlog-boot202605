package com.sprintlog.sprintlogboot;


import jdk.jfr.Enabled;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BaseEntity의 @CreatedDate, @LastModifiedDate 자동 채움 기능을 켠다.
// 이거 없으면 null로 들dj감
@EnableJpaAuditing
@SpringBootApplication
public class SprintlogBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintlogBootApplication.class, args);
    }
}
