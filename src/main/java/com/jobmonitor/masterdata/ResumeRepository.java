package com.jobmonitor.masterdata;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    @Query("""
            select new com.jobmonitor.masterdata.ResumeSummary(r.id, r.fileName, r.contentType, r.sizeBytes, r.active, r.uploadedAt)
            from Resume r order by r.uploadedAt desc""")
    List<ResumeSummary> findSummaries();

    boolean existsByActiveTrue();

    java.util.Optional<Resume> findFirstByActiveTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Resume r set r.active = false where r.active = true")
    int deactivateAll();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Resume r set r.active = true where r.id = :id")
    int activate(@Param("id") long id);
}
