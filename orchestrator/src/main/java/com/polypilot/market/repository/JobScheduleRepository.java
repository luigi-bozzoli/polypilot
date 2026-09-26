package com.polypilot.market.repository;

import com.polypilot.market.entity.JobSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobScheduleRepository extends JpaRepository<JobSchedule, String> {
    JobSchedule getByJobName(String jobName);
}
