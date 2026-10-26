-- ==============================================================================
-- Migração V5: Torna a coluna slices opcional (permite NULL) na tabela pizza_sizes
-- Permite o cadastro de tamanhos para outros produtos (bebidas, sucos, porções)
-- ==============================================================================

ALTER TABLE pizza_sizes ALTER COLUMN slices DROP NOT NULL;
