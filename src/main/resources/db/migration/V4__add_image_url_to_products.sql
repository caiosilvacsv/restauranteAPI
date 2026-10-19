-- Migração V4: Adiciona o campo image_url na tabela products
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
