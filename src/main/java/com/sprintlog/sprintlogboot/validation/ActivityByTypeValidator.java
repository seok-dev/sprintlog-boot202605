package com.sprintlog.sprintlogboot.validation;

import com.sprintlog.sprintlogboot.dto.request.CreateActivityRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// ConstraintValidator: Bean Validation이 제공하는 표준 인터페이스. <연결할 커스텀 어노테이션, 검증 대상 객체 타입(DTO)>
public class ActivityByTypeValidator implements ConstraintValidator<ValidActivityByType, CreateActivityRequest> {

    @Override
    public boolean isValid(CreateActivityRequest req, ConstraintValidatorContext context) {
        // Type 자체가 null인 경우에는 @NotNull이 잡는다 (여기서는 통과 시킨다.)
        if (req == null || req.type() == null) {
            return true;
        }
        return switch (req.type()){
            case LECTURE -> requiredField(context, isBlank(req.instructorName()), "instructorName", "강의는 강사 이름이 필요합니다.");
            case PRACTICE -> requiredField(context, req.completionRate() == null, "completionRate", "실습은 완료율이 필요합니다.");
            case READING -> requiredField(context, isBlank(req.bookTitle()),"bookTitle", "책 제목이 필요합니다.");
        };
    }

    private boolean requiredField(ConstraintValidatorContext ctx, boolean missing, String field, String message) {
        if (!missing) return true;
        ctx.disableDefaultConstraintViolation(); // 기본 메시지 비활성화
        ctx.buildConstraintViolationWithTemplate(message) // 우리 메시지를 세팅
                .addPropertyNode(field) // 메시지를 필드에 달아준다.
                .addConstraintViolation(); // 새로운 제약 조건을 세팅

        return false;
    }

    private boolean isBlank(String s){
        return s ==null || s.isBlank();
    }
}
