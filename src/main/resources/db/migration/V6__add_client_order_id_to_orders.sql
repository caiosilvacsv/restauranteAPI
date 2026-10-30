-- ==============================================================================
-- Migração V6: Adiciona coluna client_order_id na tabela orders
-- Suporta identificador único do cliente para idempotência e prevenção de duplicidade
-- ==============================================================================

ALTER TABLE orders ADD COLUMN IF NOT EXISTS client_order_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_orders_client_order_id ON orders(client_order_id);
