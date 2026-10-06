-- V1__create_tables.sql
-- Первая миграция: создание всех таблиц

-- ============================================
-- users
-- ============================================
CREATE TABLE users (
                       user_id         BIGSERIAL       PRIMARY KEY,
                       user_email      VARCHAR(255)    NOT NULL UNIQUE,
                       hash_password   VARCHAR(255)    NOT NULL,
                       created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- chats
-- ============================================
CREATE TABLE chats (
                       chat_id         BIGSERIAL       PRIMARY KEY,
                       chat_name       VARCHAR(255)    NOT NULL,
                       user_id         BIGINT          NOT NULL,
                       created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_chats_user
                           FOREIGN KEY (user_id) REFERENCES users (user_id)
                               ON DELETE CASCADE
);

CREATE INDEX idx_chats_user_id ON chats (user_id);

-- ============================================
-- messages
-- ============================================
CREATE TABLE messages (
                          message_id      BIGSERIAL       PRIMARY KEY,
                          content         TEXT            NOT NULL,
                          content_type    VARCHAR(20)     NOT NULL,   -- TEXT | VOICE
                          role            VARCHAR(20)     NOT NULL,   -- USER | ASSISTANT
                          transcription   TEXT,                       -- только для VOICE
                          user_id         BIGINT,                     -- NULL для сообщений ассистента
                          chat_id         BIGINT          NOT NULL,
                          created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_messages_user
                              FOREIGN KEY (user_id) REFERENCES users (user_id)
                                  ON DELETE SET NULL,

                          CONSTRAINT fk_messages_chat
                              FOREIGN KEY (chat_id) REFERENCES chats (chat_id)
                                  ON DELETE CASCADE,

                          CONSTRAINT chk_messages_content_type
                              CHECK (content_type IN ('TEXT', 'VOICE')),

                          CONSTRAINT chk_messages_role
                              CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_messages_chat_id ON messages (chat_id);
CREATE INDEX idx_messages_user_id ON messages (user_id);
CREATE INDEX idx_messages_created_at ON messages (created_at);

-- ============================================
-- message_feedbacks
-- ============================================
CREATE TABLE message_feedbacks (
                                   message_feedback_id     BIGSERIAL       PRIMARY KEY,
                                   message_id              BIGINT          NOT NULL UNIQUE,
                                   overall_score           SMALLINT,       -- 0–100
                                   grammar_score           SMALLINT,
                                   vocabulary_score        SMALLINT,
                                   naturalness_score       SMALLINT,
                                   corrected_text          TEXT,
                                   explanation             TEXT,
                                   created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_feedbacks_message
                                       FOREIGN KEY (message_id) REFERENCES messages (message_id)
                                           ON DELETE CASCADE
);

CREATE INDEX idx_feedbacks_message_id ON message_feedbacks (message_id);

-- ============================================
-- refresh_tokens
-- ============================================
CREATE TABLE refresh_tokens (
                                token_id        BIGSERIAL       PRIMARY KEY,
                                user_id         BIGINT          NOT NULL,
                                token_hash      VARCHAR(64)     NOT NULL UNIQUE,  -- SHA-256 hex (64 символа)
                                device_info     TEXT,                             -- user-agent / device / IP
                                expires_at      TIMESTAMP       NOT NULL,
                                created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                is_revoked      BOOLEAN         NOT NULL DEFAULT FALSE,

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id) REFERENCES users (user_id)
                                        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);