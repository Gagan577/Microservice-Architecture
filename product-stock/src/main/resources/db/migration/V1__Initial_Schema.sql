-- ============================================
-- PRODUCT STOCK SERVICE - INITIAL SCHEMA
-- Version: V1
-- Description: Creates core product and stock tables
-- ============================================

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS product_schema;

-- Set search path
SET search_path TO product_schema;

-- ============================================
-- PRODUCTS TABLE
-- ============================================
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    subcategory VARCHAR(100),
    brand VARCHAR(100),
    sku VARCHAR(100),
    barcode VARCHAR(100),
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    cost_price DECIMAL(15,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    weight DECIMAL(10,3),
    weight_unit VARCHAR(10) DEFAULT 'kg',
    dimensions JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_perishable BOOLEAN DEFAULT FALSE,
    shelf_life_days INT,
    min_stock_level INT DEFAULT 0,
    max_stock_level INT,
    reorder_point INT DEFAULT 10,
    reorder_quantity INT DEFAULT 100,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for products
CREATE INDEX idx_products_product_code ON products(product_code);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_sku ON products(sku) WHERE sku IS NOT NULL;
CREATE INDEX idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_products_brand ON products(brand) WHERE brand IS NOT NULL;
CREATE INDEX idx_products_name_search ON products USING gin(to_tsvector('english', name));

-- ============================================
-- STOCK TABLE (Current Stock Levels)
-- ============================================
CREATE TABLE stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    warehouse_code VARCHAR(50) NOT NULL DEFAULT 'MAIN',
    location_code VARCHAR(50),
    total_quantity INT NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
    available_quantity INT NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    damaged_quantity INT NOT NULL DEFAULT 0 CHECK (damaged_quantity >= 0),
    in_transit_quantity INT NOT NULL DEFAULT 0 CHECK (in_transit_quantity >= 0),
    last_counted_at TIMESTAMP WITH TIME ZONE,
    last_restocked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(product_id, warehouse_code),
    CONSTRAINT chk_stock_quantities CHECK (
        available_quantity + reserved_quantity + damaged_quantity <= total_quantity
    )
);

-- Indexes for stock
CREATE INDEX idx_stock_product_id ON stock(product_id);
CREATE INDEX idx_stock_warehouse ON stock(warehouse_code);
CREATE INDEX idx_stock_available ON stock(available_quantity);
CREATE INDEX idx_stock_product_warehouse ON stock(product_id, warehouse_code);

-- ============================================
-- STOCK RESERVATIONS TABLE
-- ============================================
CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    stock_id UUID NOT NULL REFERENCES stock(id),
    warehouse_code VARCHAR(50) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    order_reference VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reserved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE,
    committed_at TIMESTAMP WITH TIME ZONE,
    release_reason TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for stock_reservations
CREATE INDEX idx_reservations_product_id ON stock_reservations(product_id);
CREATE INDEX idx_reservations_stock_id ON stock_reservations(stock_id);
CREATE INDEX idx_reservations_order_ref ON stock_reservations(order_reference);
CREATE INDEX idx_reservations_status ON stock_reservations(status);
CREATE INDEX idx_reservations_expires_at ON stock_reservations(expires_at) WHERE status = 'ACTIVE';

-- ============================================
-- STOCK MOVEMENTS TABLE (Audit Trail)
-- ============================================
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    stock_id UUID NOT NULL REFERENCES stock(id),
    movement_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    reference_code VARCHAR(100),
    reason TEXT,
    performed_by VARCHAR(100),
    trace_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for stock_movements
CREATE INDEX idx_movements_product_id ON stock_movements(product_id);
CREATE INDEX idx_movements_stock_id ON stock_movements(stock_id);
CREATE INDEX idx_movements_type ON stock_movements(movement_type);
CREATE INDEX idx_movements_reference ON stock_movements(reference_type, reference_id);
CREATE INDEX idx_movements_created_at ON stock_movements(created_at DESC);
CREATE INDEX idx_movements_trace_id ON stock_movements(trace_id);

-- ============================================
-- AUDIT LOG TABLE
-- ============================================
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    trace_id VARCHAR(64),
    ip_address VARCHAR(45),
    user_agent TEXT
);

-- Indexes for audit_log
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at DESC);
CREATE INDEX idx_audit_log_trace_id ON audit_log(trace_id);

-- ============================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_stock_updated_at
    BEFORE UPDATE ON stock
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reservations_updated_at
    BEFORE UPDATE ON stock_reservations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================
COMMENT ON TABLE products IS 'Master product catalog';
COMMENT ON TABLE stock IS 'Current stock levels per product and warehouse';
COMMENT ON TABLE stock_reservations IS 'Temporary stock reservations for orders';
COMMENT ON TABLE stock_movements IS 'Audit trail of all stock changes';
COMMENT ON TABLE audit_log IS 'General audit trail for entity changes';

COMMENT ON COLUMN products.status IS 'Product status: ACTIVE, INACTIVE, DISCONTINUED, OUT_OF_STOCK';
COMMENT ON COLUMN stock_reservations.status IS 'Reservation status: ACTIVE, RELEASED, COMMITTED, EXPIRED';
COMMENT ON COLUMN stock_movements.movement_type IS 'Types: RECEIVED, RESERVED, RELEASED, SOLD, RETURNED, ADJUSTED, DAMAGED, TRANSFERRED';
