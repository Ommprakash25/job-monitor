package com.jobmonitor.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** How far each folder has been read, so a sync only fetches what's new. */
@Entity
@Table(name = "inbox_sync_state")
public class InboxSyncState {

    @Id
    @Column(length = 64)
    private String folder;

    @Column(name = "uid_validity", nullable = false)
    private long uidValidity;

    @Column(name = "last_uid", nullable = false)
    private long lastUid;

    protected InboxSyncState() {
    }

    public InboxSyncState(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }

    public long getUidValidity() {
        return uidValidity;
    }

    public void setUidValidity(long uidValidity) {
        this.uidValidity = uidValidity;
    }

    public long getLastUid() {
        return lastUid;
    }

    public void setLastUid(long lastUid) {
        this.lastUid = lastUid;
    }
}
