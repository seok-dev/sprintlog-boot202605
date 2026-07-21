package com.sprintlog.sprintlogboot.service;

import com.sprintlog.sprintlogboot.domain.ActivityAuditLog;
import com.sprintlog.sprintlogboot.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // 시도 이력을 독립된 트랜잭션으로 남긴다.
    // 활동 등록 로직이 별도의 트랜잭션을 가지고 있어도 이 메서드는 자기만의 트랜잭션을 새로 열 것이다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAttempt(String action, String detail){
        auditLogRepository.save(new ActivityAuditLog(action, detail));
    }

}
