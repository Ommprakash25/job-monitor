-- job-monitor, later phases: job listings from job APIs.

CREATE TABLE job_listing (
    id           BIGSERIAL     PRIMARY KEY,
    source       VARCHAR(30)   NOT NULL,
    external_id  VARCHAR(200)  NOT NULL,
    title        VARCHAR(300)  NOT NULL,
    company      VARCHAR(300)  NOT NULL DEFAULT '',
    location     VARCHAR(300)  NOT NULL DEFAULT '',
    url          TEXT          NOT NULL DEFAULT '',
    posted_at    TIMESTAMPTZ,
    description  TEXT          NOT NULL DEFAULT '',
    found_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT job_listing_unique UNIQUE (source, external_id)
);
CREATE INDEX job_listing_posted ON job_listing (posted_at);
