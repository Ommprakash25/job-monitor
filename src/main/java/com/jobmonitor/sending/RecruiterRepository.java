package com.jobmonitor.sending;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {

    Optional<Recruiter> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Recruiter> findAllByOrderByEmailAsc();
}
