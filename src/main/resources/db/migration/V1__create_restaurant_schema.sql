CREATE TABLE restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    number INTEGER NOT NULL UNIQUE,
    description VARCHAR(100),
    capacity INTEGER NOT NULL DEFAULT 4,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CHECK ( status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'INACTIVE') )
);

CREATE TABLE category_products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE products(
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT,
    name VARCHAR(100),
    description TEXT,
    price NUMERIC(19, 2) NOT NULL CHECK ( price >= 0 ),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    preparation_time_minutes INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,

    CONSTRAINT fk_category_id
                     FOREIGN KEY (category_id)
                     REFERENCES category_products(id)
                     ON DELETE SET NULL
);

CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category_id);

CREATE TABLE orders(
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT,
    opened_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'OPENED',
    notes TEXT,
    CHECK( status IN ('OPENED', 'PREPARING', 'READY', 'DESPATCHED', 'DELIVERED', 'CLOSED', 'CANCELLED') ),

    CONSTRAINT fk_table_id
                   FOREIGN KEY (table_id)
                   REFERENCES restaurant_tables(id)
                   ON DELETE SET NULL
);

CREATE INDEX idx_order_tables ON orders(table_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_opened ON orders(opened_at);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK ( quantity > 0 ),
    unit_price NUMERIC(19, 2) NOT NULL CHECK ( unit_price > 0 ),
    preparation_started_at TIMESTAMPTZ,
    ready_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    CHECK( status IN ('PENDING', 'PREPARING', 'READY', 'DESPATCHED', 'DELIVERED', 'CLOSED', 'CANCELLED') ),

    CONSTRAINT fk_order_id
                         FOREIGN KEY (order_id)
                         REFERENCES orders(id)
                         ON DELETE CASCADE,
    CONSTRAINT fk_product_id
                         FOREIGN KEY (product_id)
                         REFERENCES products(id)
                         ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_order_items_status ON order_items(status);

CREATE TABLE payment(
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK ( amount > 0 ),
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    external_transaction_code VARCHAR(100),
    payment_date TIMESTAMPTZ,
    create_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CHECK ( method IN ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'PIX') ),
    CHECK ( status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED') ),

    CONSTRAINT fk_order_id
                    FOREIGN KEY (order_id)
                    REFERENCES orders(id)
                    ON DELETE RESTRICT
);

CREATE INDEX idx_payment_order ON payment(order_id);
CREATE INDEX idx_payment_status ON payment(status);

CREATE TABLE invoices(
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    subtotal NUMERIC(19, 2) NOT NULL CHECK ( subtotal >= 0 ),
    service_fee NUMERIC(19, 2) NOT NULL DEFAULT 0.00 CHECK ( service_fee >= 0 ),
    discount NUMERIC(19, 2) NOT NULL DEFAULT 0.00 CHECK ( discount >= 0 ),
    total_amount NUMERIC(19, 2) NOT NULL CHECK ( total_amount >= 0 ),
    closed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_id
                     FOREIGN KEY (order_id)
                     REFERENCES orders(id)
                     ON DELETE RESTRICT
);

CREATE TABLE ingredients(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    price NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_ingredients_name ON ingredients(name);

CREATE TABLE product_ingredients(
    ingredient_id BIGINT,
    product_id BIGINT,

    PRIMARY KEY (ingredient_id, product_id),

    CONSTRAINT fk_ingredient_id
                        FOREIGN KEY (ingredient_id)
                        REFERENCES ingredients(id)
                        ON DELETE CASCADE,

    CONSTRAINT fk_product_id
                        FOREIGN KEY (product_id)
                        REFERENCES products(id)
                        ON DELETE CASCADE
);

CREATE TABLE order_item_modifications(
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK ( quantity > 0 ),
    extra_price NUMERIC(19,2) NOT NULL DEFAULT 00.0 CHECK ( extra_price >= 0 ),

    CONSTRAINT fk_order_item_id
                    FOREIGN KEY (order_item_id)
                    REFERENCES order_items(id)
                    ON DELETE CASCADE,

    CONSTRAINT fk_ingredient_id
                    FOREIGN KEY (ingredient_id)
                    REFERENCES ingredients(id)
                    ON DELETE RESTRICT,
    CONSTRAINT chk_modification_type
                    CHECK ( type IN ('ADD', 'REMOVE') )
);

CREATE INDEX idx_order_modification_item ON order_item_modifications(order_item_id);