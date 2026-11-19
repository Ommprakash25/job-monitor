-- job-monitor, phase 4: the inbox, reply and bounce tracking.

-- The Message-ID Gmail gave each sent email, so replies can be matched to it.
ALTER TABLE email_log ADD COLUMN message_id VARCHAR(998);
ALTER TABLE email_log ADD COLUMN replied_at TIMESTAMPTZ;
ALTER TABLE email_log ADD COLUMN bounced_at TIMESTAMPTZ;
CREATE INDEX email_log_message_id ON email_log (message_id);

CREATE TABLE inbox_message (
    id              BIGSERIAL     PRIMARY KEY,
    folder          VARCHAR(10)   NOT NULL,
    imap_uid        BIGINT        NOT NULL,
    message_id      VARCHAR(998),
    from_address    VARCHAR(254)  NOT NULL DEFAULT '',
    from_name       VARCHAR(255)  NOT NULL DEFAULT '',
    to_address      VARCHAR(998)  NOT NULL DEFAULT '',
    subject         VARCHAR(998)  NOT NULL DEFAULT '',
    received_at     TIMESTAMPTZ   NOT NULL,
    body_text       TEXT          NOT NULL DEFAULT '',
    attachment_names TEXT         NOT NULL DEFAULT '',
    category        VARCHAR(20)   NOT NULL,
    link_method     VARCHAR(20),
    recruiter_id    BIGINT        REFERENCES recruiter (id),
    company_id      BIGINT        REFERENCES company (id),
    email_log_id    BIGINT        REFERENCES email_log (id),
    synced_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX inbox_message_message_id ON inbox_message (message_id);
CREATE INDEX inbox_message_received ON inbox_message (received_at);

CREATE TABLE inbox_sync_state (
    folder        VARCHAR(64) PRIMARY KEY,
    uid_validity  BIGINT      NOT NULL DEFAULT 0,
    last_uid      BIGINT      NOT NULL DEFAULT 0
);
