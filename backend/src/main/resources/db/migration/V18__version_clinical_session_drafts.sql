ALTER TABLE clinical_session_drafts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
