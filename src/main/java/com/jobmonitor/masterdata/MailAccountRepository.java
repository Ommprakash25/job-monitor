package com.jobmonitor.masterdata;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

    Optional<MailAccount> findFirstByActiveTrue();
}
