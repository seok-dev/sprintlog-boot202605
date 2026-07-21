package com.sprintlog.sprintlogboot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration //이 클래스는 설정 클래스 이다. [Bean정의 모음 설정 클래스]
@EnableConfigurationProperties(SprintLogProperties.class)
public class AppConfig {

    // 반환 객체 타입이 Bean 타입(Clock), 다른 곳에서 Clock clock으로 주입받으면 이 객체가 들어옴
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
