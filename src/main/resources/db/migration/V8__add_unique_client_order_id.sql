-- ==============================================================================
-- Migração V8: Adiciona índice único parcial para client_order_id na tabela orders
-- Suporta idempotência estrita em nível de banco de dados, prevenindo pedidos gêmeos
-- sob condições de concorrência e reenvios de rede, ignorando valores nulos ou vazios.
-- ==============================================================================

-- 1. Sanitização retroativa: Se existirem duplicatas pré-existentes de client_order_id
-- na base de dados com valor não-nulo e não-vazio, mantemos o primeiro pedido (menor ID)
-- e anulamos o client_order_id dos registros duplicados posteriores para garantir que
-- a criação do índice único não falhe em nenhuma base local ou ambiente de testes.
UPDATE orders
SET client_order_id = NULL
WHERE id IN (
    SELECT o2.id
    FROM orders o1
    JOIN orders o2 ON o1.client_order_id = o2.client_order_id AND o1.id < o2.id
    WHERE o1.client_order_id IS NOT NULL 
      AND TRIM(o1.client_order_id) <> ''
);

-- 2. Remove o índice B-Tree não-único anterior criado na V6 para economizar espaço e evitar overhead
DROP INDEX IF EXISTS idx_orders_client_order_id;

-- 3. Criação do índice UNIQUE parcial no PostgreSQL
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_client_order_id
ON orders(client_order_id)
WHERE client_order_id IS NOT NULL AND client_order_id <> '';
