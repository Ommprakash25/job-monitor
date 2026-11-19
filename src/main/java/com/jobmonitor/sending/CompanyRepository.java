package com.jobmonitor.sending;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByDomain(String domain);

    Optional<Company> findFirstByDomainIsNull();
}
