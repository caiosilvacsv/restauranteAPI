-- ==============================================================================
-- Migração V2: Criação da tabela de controle de sequência diária de pedidos
-- ==============================================================================

CREATE TABLE IF NOT EXISTS daily_order_sequence (
    business_date DATE PRIMARY KEY,
    last_sequence INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
