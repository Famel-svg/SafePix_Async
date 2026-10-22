-- Controle de idempotencia do consumer de Pix.
-- Cada Pix (pela chave id) passa por PROCESSING e termina em PROCESSED.
-- Falhas removem a linha para permitir nova tentativa (retry/reprocessamento da DLQ).
CREATE TABLE pix_processing (
    pix_id     UUID         PRIMARY KEY,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_pix_processing_status CHECK (status IN ('PROCESSING', 'PROCESSED'))
);

CREATE INDEX idx_pix_processing_status ON pix_processing (status);
