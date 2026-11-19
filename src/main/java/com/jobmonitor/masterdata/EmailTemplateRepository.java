package com.jobmonitor.masterdata;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findAllByOrderByDefaultTemplateDescNameAsc();

    Optional<EmailTemplate> findFirstByDefaultTemplateTrue();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailTemplate t set t.defaultTemplate = false where t.defaultTemplate = true")
    int clearDefault();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EmailTemplate t set t.defaultTemplate = true where t.id = :id")
    int makeDefault(@Param("id") long id);
}
