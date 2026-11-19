package com.jobmonitor.jobs;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobListingRepository extends JpaRepository<JobListing, Long> {

    Optional<JobListing> findBySourceAndExternalId(String source, String externalId);

    List<JobListing> findTop200ByOrderByPostedAtDesc();
}
