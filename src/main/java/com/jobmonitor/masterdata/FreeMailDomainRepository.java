package com.jobmonitor.masterdata;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreeMailDomainRepository extends JpaRepository<FreeMailDomain, String> {

    List<FreeMailDomain> findAllByOrderByDomainAsc();
}
