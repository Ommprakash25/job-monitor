package com.jobmonitor.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxSyncStateRepository extends JpaRepository<InboxSyncState, String> {
}
