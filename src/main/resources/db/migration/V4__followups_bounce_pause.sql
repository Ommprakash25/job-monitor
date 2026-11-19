-- job-monitor, phase 5 (part): follow-ups in the same thread, and the automatic bounce pause.

ALTER TABLE sending_settings ADD COLUMN pause_reason VARCHAR(300);

-- Headers that tie a follow-up to the email it follows up.
ALTER TABLE email_log ADD COLUMN in_reply_to VARCHAR(998);
ALTER TABLE email_log ADD COLUMN thread_references VARCHAR(4000);
