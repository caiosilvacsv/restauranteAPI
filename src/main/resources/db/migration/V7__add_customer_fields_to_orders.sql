-- ==============================================================================
-- Migração V7: Adiciona colunas de snapshot do cliente diretamente na tabela orders
-- Desacopla a identificação do cliente do cadastro de usuário e permite comandas
-- de salão/balcão com o nome real do cliente sem depender do usuário logado.
-- ==============================================================================

ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_orders_customer_phone ON orders(customer_phone);

-- 1. Atualização retroativa de pedidos onde customer_name está vazio, nulo ou 'Cliente',
-- recuperando o nome real informado em payment.payer_name quando disponível.
UPDATE orders o
SET customer_name = TRIM(p.payer_name)
FROM payments p
WHERE p.order_id = o.id
  AND p.payer_name IS NOT NULL
  AND TRIM(p.payer_name) <> ''
  AND LOWER(TRIM(p.payer_name)) <> 'cliente'
  AND (o.customer_name IS NULL OR TRIM(o.customer_name) = '' OR LOWER(TRIM(o.customer_name)) = 'cliente');

-- 2. Atualização retroativa da tabela users para clientes temporários/visitantes
-- cujo nome tenha ficado como 'Cliente' ou nulo, associando o nome real resgatado dos pagamentos.
UPDATE users u
SET name = TRIM(p.payer_name)
FROM orders o
JOIN payments p ON p.order_id = o.id
WHERE o.user_id = u.id
  AND (u.name IS NULL OR TRIM(u.name) = '' OR LOWER(TRIM(u.name)) = 'cliente')
  AND p.payer_name IS NOT NULL
  AND TRIM(p.payer_name) <> ''
  AND LOWER(TRIM(p.payer_name)) <> 'cliente';
