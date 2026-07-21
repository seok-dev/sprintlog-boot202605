package com.sprintlog.sprintlogboot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 제거 가능 access접근 제한 레벨
public class ActivityAuditLog extends BaseEntity {

    // 어떤 작업이었는지 - 생성, 업데이트, 삭제
    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false, length = 300)
    private String detail;


    public ActivityAuditLog(String action, String detail) {
        this.action = action;
        this.detail = detail;
    }
}
