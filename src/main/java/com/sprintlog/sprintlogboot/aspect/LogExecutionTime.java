package com.sprintlog.sprintlogboot.aspect;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // AOP는 app이 실행되는 도중에 동적으로 측정 메서드를 가로채서 기능을 추가하기 때문에 RUNTIME으로 추가
@Target(ElementType.METHOD) // 어노테이션을 붙일 수 있는 위치는 메서드이다.
public @interface LogExecutionTime {
}
