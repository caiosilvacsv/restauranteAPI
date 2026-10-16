-- ==============================================================================
-- Schema Unificado Inicial - Sistema de Restaurante / Pizzaria & KDS
-- Migração Flyway: V1__initial_schema.sql
-- ==============================================================================
-- ATENÇÃO: Esta migração contém estritamente definições de estrutura DDL (tabelas,
-- índices e restrições de integridade). Nenhum dado, usuário com senha padrão ou
-- configuração sensível é inserido aqui, garantindo segurança em produção.
-- ==============================================================================

-- 1. Configurações da Pizzaria (RestaurantConfig)
CREATE TABLE IF NOT EXISTS restaurant_config (
    id BIGSERIAL PRIMARY KEY,
    restaurant_name VARCHAR(150) NOT NULL DEFAULT 'La Bella Pizzaria',
    whatsapp_phone VARCHAR(30) NOT NULL,
    pix_key VARCHAR(100),
    type_pix_key VARCHAR(30) DEFAULT 'TELEFONE',
    delivery_fee NUMERIC(19, 2) NOT NULL DEFAULT 2.00 CHECK (delivery_fee >= 0),
    address VARCHAR(200),
    open_time TIME DEFAULT '18:00:00',
    close_time TIME DEFAULT '23:30:00',
    open_days VARCHAR(120) DEFAULT 'TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY',
    is_manually_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_message TEXT DEFAULT 'Estamos fechados no momento. Não estamos recebendo pedidos!',
    estimated_time_minutes INTEGER DEFAULT 40,
    estimated_time_text VARCHAR(50) DEFAULT '40-50 min'
);

-- 2. Mesas do Restaurante (RestaurantTables)
CREATE TABLE IF NOT EXISTS restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    number INTEGER NOT NULL UNIQUE,
    description VARCHAR(100),
    capacity INTEGER NOT NULL DEFAULT 4,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'INACTIVE'))
);

-- 3. Categorias de Produtos (CategoryProduct)
CREATE TABLE IF NOT EXISTS category_products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 4. Tamanhos de Pizza (PizzaSize)
CREATE TABLE IF NOT EXISTS pizza_sizes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    short_name VARCHAR(10) NOT NULL UNIQUE,
    slices INTEGER NOT NULL,
    max_flavors INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Catálogo de Produtos (Product)
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    allows_half_and_half BOOLEAN NOT NULL DEFAULT FALSE,
    preparation_time_minutes INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id)
        REFERENCES category_products(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);

-- 6. Preços de Produtos por Tamanho (ProductSizePrice)
CREATE TABLE IF NOT EXISTS product_size_prices (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    pizza_size_id BIGINT NOT NULL,
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),

    CONSTRAINT fk_psp_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_psp_pizza_size
        FOREIGN KEY (pizza_size_id)
        REFERENCES pizza_sizes(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_product_size
        UNIQUE (product_id, pizza_size_id)
);

CREATE INDEX IF NOT EXISTS idx_psp_product ON product_size_prices(product_id);
CREATE INDEX IF NOT EXISTS idx_psp_size ON product_size_prices(pizza_size_id);

-- 7. Ingredientes e Adicionais (Ingredient)
CREATE TABLE IF NOT EXISTS ingredients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS product_ingredients (
    product_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, ingredient_id),

    CONSTRAINT fk_pi_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pi_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(id)
        ON DELETE CASCADE
);

-- 8. Usuários e Autenticação (User)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    login TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    name VARCHAR(100),
    role TEXT NOT NULL,
    number TEXT NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_users_login ON users(login);

-- 9. Endereços Legados (Address)
CREATE TABLE IF NOT EXISTS address (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    street VARCHAR(100) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(30),
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    reference_point TEXT,

    CONSTRAINT fk_address_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- 10. Carteira de Endereços do Usuário (UserAddress)
CREATE TABLE IF NOT EXISTS user_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(50),
    street VARCHAR(150) NOT NULL,
    number VARCHAR(20) NOT NULL,
    neighborhood VARCHAR(100) NOT NULL,
    complement VARCHAR(100),
    reference_point TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,

    CONSTRAINT fk_user_addresses_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_addresses_user ON user_addresses(user_id);

-- 11. Pedidos (Order)
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT,
    user_id BIGINT,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'OPENED',
    notes TEXT,
    order_type VARCHAR(10) NOT NULL DEFAULT 'DINE_IN',
    order_code VARCHAR(10),
    payment_method VARCHAR(30),
    cash_change_for NUMERIC(19, 2),
    street VARCHAR(150),
    number VARCHAR(20),
    neighborhood VARCHAR(100),
    complement VARCHAR(100),
    reference_point TEXT,
    cancel_reason TEXT,
    estimated_minutes INTEGER,
    estimated_delivery_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CHECK (status IN ('OPENED', 'PREPARING', 'READY', 'DESPATCHED', 'DELIVERED', 'CLOSED', 'CANCELLED')),

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_orders_table
        FOREIGN KEY (table_id)
        REFERENCES restaurant_tables(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_order_tables ON orders(table_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_opened ON orders(opened_at);
CREATE INDEX IF NOT EXISTS idx_order_updated_at ON orders(updated_at);

-- 12. Itens do Pedido (OrderItem)
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    second_product_id BIGINT,
    pizza_size_id BIGINT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL CHECK (unit_price > 0),
    preparation_started_at TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    notes TEXT,
    notes_first_half TEXT,
    notes_second_half TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    CHECK (status IN ('PENDING', 'QUEUED', 'PREPARING', 'READY', 'DESPATCHED', 'DELIVERED', 'CLOSED', 'CANCELLED')),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_second_product
        FOREIGN KEY (second_product_id)
        REFERENCES products(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_pizza_size
        FOREIGN KEY (pizza_size_id)
        REFERENCES pizza_sizes(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_status ON order_items(status);

-- 13. Modificações dos Itens (OrderItemModification)
CREATE TABLE IF NOT EXISTS order_item_modifications (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('ADD', 'REMOVE')),
    extra_price NUMERIC(19, 2) NOT NULL DEFAULT 0.00 CHECK (extra_price >= 0),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),

    CONSTRAINT fk_oim_item
        FOREIGN KEY (order_item_id)
        REFERENCES order_items(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_oim_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredients(id)
        ON DELETE RESTRICT
);

-- 14. Faturas / Fechamentos Contábeis (Invoice)
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    subtotal NUMERIC(19, 2) NOT NULL CHECK (subtotal >= 0),
    service_fee NUMERIC(19, 2) NOT NULL DEFAULT 0.00 CHECK (service_fee >= 0),
    discount NUMERIC(19, 2) NOT NULL DEFAULT 0.00 CHECK (discount >= 0),
    delivery_fee NUMERIC(19, 2) NOT NULL DEFAULT 2.00 CHECK (delivery_fee >= 0),
    total_amount NUMERIC(19, 2) NOT NULL CHECK (total_amount >= 0),
    closed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoices_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
);

-- 15. Pagamentos e Divisão de Contas (Payment)
CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    invoice_id BIGINT,
    payer_name VARCHAR(100),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    method VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    payment_date TIMESTAMPTZ,
    external_transaction_code VARCHAR(100),
    checkout_id VARCHAR(100),
    valid_until TIMESTAMPTZ,
    create_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_payment_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_payment_order ON payment(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(status);
