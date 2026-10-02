package com.medilink.repository;

import com.medilink.model.support.ReportedIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportedIssueRepository extends JpaRepository<ReportedIssue, String> {

    List<ReportedIssue> findAllByOrderByCreatedAtDesc();

    List<ReportedIssue> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ReportedIssue> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
