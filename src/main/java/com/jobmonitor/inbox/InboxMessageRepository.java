package com.jobmonitor.inbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxMessageRepository extends JpaRepository<InboxMessage, Long> {

    List<InboxMessage> findTop500ByOrderByReceivedAtDesc();

    boolean existsByMessageId(String messageId);

    boolean existsByFolderAndImapUid(String folder, long imapUid);
}
