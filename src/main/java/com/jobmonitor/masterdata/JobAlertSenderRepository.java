package com.jobmonitor.masterdata;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAlertSenderRepository extends JpaRepository<JobAlertSender, String> {

    List<JobAlertSender> findAllByOrderByPatternAsc();
}
