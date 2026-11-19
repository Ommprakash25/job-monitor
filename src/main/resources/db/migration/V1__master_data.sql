-- job-monitor, phase 1: master data.

-- The person applying. Always exactly one row.
CREATE TABLE profile (
    id          INTEGER      PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    full_name   VARCHAR(120) NOT NULL DEFAULT '',
    phone       VARCHAR(40)  NOT NULL DEFAULT '',
    signature   TEXT         NOT NULL DEFAULT '',
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
INSERT INTO profile (id) VALUES (1);

-- Sending mailboxes. The app password or sign-in token is stored encrypted.
CREATE TABLE mail_account (
    id                 BIGSERIAL    PRIMARY KEY,
    email_address      VARCHAR(254) NOT NULL UNIQUE,
    auth_type          VARCHAR(20)  NOT NULL DEFAULT 'APP_PASSWORD'
                       CHECK (auth_type IN ('APP_PASSWORD', 'GOOGLE_SIGN_IN')),
    secret_encrypted   TEXT,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    last_check_at      TIMESTAMPTZ,
    last_check_ok      BOOLEAN,
    last_check_message VARCHAR(500),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX mail_account_one_active ON mail_account (active) WHERE active;

-- When and how fast the app sends. Always exactly one row.
CREATE TABLE sending_settings (
    id                   INTEGER      PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    sending_days         VARCHAR(80)  NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    window_start         TIME         NOT NULL DEFAULT '10:00',
    window_end           TIME         NOT NULL DEFAULT '18:00',
    time_zone            VARCHAR(64)  NOT NULL DEFAULT 'Asia/Kolkata',
    gap_min_minutes      INTEGER      NOT NULL DEFAULT 8,
    gap_max_minutes      INTEGER      NOT NULL DEFAULT 32,
    daily_limit          INTEGER      NOT NULL DEFAULT 15,
    recontact_after_days INTEGER      DEFAULT 30,   -- NULL: never email the same recruiter twice
    company_limit_count  INTEGER      NOT NULL DEFAULT 2,
    company_limit_days   INTEGER      NOT NULL DEFAULT 7,
    follow_up_after_days INTEGER      NOT NULL DEFAULT 7,
    bounce_pause_percent INTEGER      DEFAULT 5,    -- NULL: bounce pause off
    role_fallback        VARCHAR(120) NOT NULL DEFAULT 'Software Engineer',
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT sending_gap_range CHECK (gap_min_minutes BETWEEN 1 AND 120
                                        AND gap_max_minutes BETWEEN 1 AND 120
                                        AND gap_min_minutes <= gap_max_minutes),
    CONSTRAINT sending_window_order CHECK (window_start < window_end),
    CONSTRAINT sending_daily_limit_range CHECK (daily_limit BETWEEN 1 AND 500),
    CONSTRAINT sending_recontact_range CHECK (recontact_after_days IS NULL OR recontact_after_days BETWEEN 1 AND 3650),
    CONSTRAINT sending_company_limit_range CHECK (company_limit_count BETWEEN 1 AND 100
                                                  AND company_limit_days BETWEEN 1 AND 365),
    CONSTRAINT sending_follow_up_range CHECK (follow_up_after_days BETWEEN 1 AND 60),
    CONSTRAINT sending_bounce_range CHECK (bounce_pause_percent IS NULL OR bounce_pause_percent BETWEEN 1 AND 50)
);
INSERT INTO sending_settings (id) VALUES (1);

-- Dates when nothing is sent, such as public holidays.
CREATE TABLE day_off (
    id    BIGSERIAL    PRIMARY KEY,
    day   DATE         NOT NULL UNIQUE,
    note  VARCHAR(200) NOT NULL DEFAULT ''
);

-- Uploaded resumes. Exactly one is attached to emails: the active one.
CREATE TABLE resume (
    id           BIGSERIAL    PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   INTEGER      NOT NULL,
    content      BYTEA        NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX resume_one_active ON resume (active) WHERE active;

-- Email templates. {name}, {company} and {role} are filled in per recruiter.
CREATE TABLE email_template (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    subject     VARCHAR(255) NOT NULL,
    body        TEXT         NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX email_template_one_default ON email_template (is_default) WHERE is_default;

INSERT INTO email_template (name, subject, body, is_default) VALUES (
    'Starter template',
    'Application for {role} – [X] years of Java / Spring Boot',
    E'Hi {name},\n\n'
    || E'I''m a Java backend developer with **[X] years of Spring Boot and microservices** experience, '
    || E'and I''d like to apply for the {role} opening at {company}.\n\n'
    || E'[One line on your strongest relevant achievement.]\n\n'
    || E'I can join **[notice period]**. My resume is attached.\n\n'
    || E'LinkedIn: [your LinkedIn link]\n'
    || E'GitHub: [your GitHub link]\n\n'
    || E'Regards,\n'
    || E'[Your name]\n'
    || E'[Phone]',
    TRUE);

-- Senders on these domains get the company "Others".
CREATE TABLE free_mail_domain (
    domain VARCHAR(253) PRIMARY KEY
);
INSERT INTO free_mail_domain (domain) VALUES
    ('gmail.com'), ('googlemail.com'), ('yahoo.com'), ('yahoo.co.in'), ('yahoo.in'), ('ymail.com'),
    ('rocketmail.com'), ('outlook.com'), ('outlook.in'), ('hotmail.com'), ('live.com'), ('msn.com'),
    ('rediffmail.com'), ('icloud.com'), ('me.com'), ('aol.com'), ('protonmail.com'), ('proton.me'),
    ('zohomail.com'), ('zohomail.in'), ('gmx.com'), ('mail.com'), ('yandex.com');

-- Email from these senders is filed as a job-portal alert, not a conversation.
-- Each entry is a domain (naukri.com) or a full address.
CREATE TABLE job_alert_sender (
    pattern VARCHAR(254) PRIMARY KEY
);
INSERT INTO job_alert_sender (pattern) VALUES
    ('naukri.com'), ('linkedin.com'), ('indeed.com'), ('glassdoor.com'), ('glassdoor.co.in'),
    ('foundit.in'), ('shine.com'), ('instahyre.com'), ('iimjobs.com'), ('hirist.tech'),
    ('cutshort.io'), ('wellfound.com');
