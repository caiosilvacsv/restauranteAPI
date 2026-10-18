-- ==============================================================================
-- Migração V3: Adiciona o campo price (preço do tamanho) na tabela pizza_sizes
-- ==============================================================================

ALTER TABLE pizza_sizes ADD COLUMN IF NOT EXISTS price NUMERIC(19, 2) NOT NULL DEFAULT 0.00;
