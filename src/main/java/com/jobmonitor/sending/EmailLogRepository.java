package com.jobmonitor.sending;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Optional<EmailLog> findFirstByStatusAndDueAtLessThanEqualOrderByDueAtAsc(SendStatus status, Instant now);

    long countByStatusAndSentAtGreaterThanEqual(SendStatus status, Instant since);

    long countByRecruiterAndStatusAndSentAtGreaterThanEqual(Recruiter recruiter, SendStatus status, Instant since);

    long countByCompanyAndStatusAndSentAtGreaterThanEqual(Company company, SendStatus status, Instant since);

    boolean existsByRecruiterAndStatus(Recruiter recruiter, SendStatus status);

    long countByStatus(SendStatus status);

    List<EmailLog> findTop200ByOrderByQueuedAtDesc();

    Optional<EmailLog> findFirstByMessageId(String messageId);

    long countByBouncedAtGreaterThanEqual(Instant since);

    long countByBouncedAtIsNotNull();

    long countByRepliedAtIsNotNull();

    long countByStatusAndSentAtBeforeAndRepliedAtIsNullAndBouncedAtIsNull(SendStatus status, Instant before);

    List<EmailLog> findTop100ByStatusAndSentAtBeforeAndRepliedAtIsNullAndBouncedAtIsNullOrderBySentAtAsc(
            SendStatus status, Instant before);

    @Query("select distinct l.recruiter.id from EmailLog l where l.status = :status")
    Set<Long> findRecruiterIdsWithStatus(@Param("status") SendStatus status);
}
