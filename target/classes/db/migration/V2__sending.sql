-- job-monitor, phase 2: recruiters, companies, the email log and sending state.

-- Sending stays paused until the user presses "Start sending".
ALTER TABLE sending_settings ADD COLUMN sending_paused BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE sending_settings ADD COLUMN next_send_at TIMESTAMPTZ;

CREATE TABLE company (
    id      BIGSERIAL    PRIMARY KEY,
    name    VARCHAR(200) NOT NULL,
    domain  VARCHAR(253) UNIQUE
);

-- Free-mail recruiters, and anyone without a company, belong here (no domain).
INSERT INTO company (name, domain) VALUES ('Others', NULL);

CREATE TABLE recruiter (
    id              BIGSERIAL    PRIMARY KEY,
    email           VARCHAR(254) NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL DEFAULT '',
    company_id      BIGINT       NOT NULL REFERENCES company (id),
    source          VARCHAR(20)  NOT NULL,
    do_not_contact  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE email_log (
    id                    BIGSERIAL    PRIMARY KEY,
    mail_account_address  VARCHAR(254) NOT NULL,
    recruiter_id          BIGINT       NOT NULL REFERENCES recruiter (id),
    company_id            BIGINT       NOT NULL REFERENCES company (id),
    to_address            VARCHAR(254) NOT NULL,
    subject               VARCHAR(255) NOT NULL,
    text_body             TEXT         NOT NULL,
    html_body             TEXT         NOT NULL,
    attachment_name       VARCHAR(255),
    template_name         VARCHAR(120),
    channel               VARCHAR(20)  NOT NULL,
    status                VARCHAR(12)  NOT NULL CHECK (status IN ('QUEUED', 'SENT', 'FAILED', 'SKIPPED')),
    error                 TEXT,
    attempts              INTEGER      NOT NULL DEFAULT 0,
    queued_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    due_at                TIMESTAMPTZ  NOT NULL,
    sent_at               TIMESTAMPTZ
);
CREATE INDEX email_log_due ON email_log (status, due_at);
CREATE INDEX email_log_recruiter_sent ON email_log (recruiter_id, status, sent_at);
CREATE INDEX email_log_company_sent ON email_log (company_id, status, sent_at);
