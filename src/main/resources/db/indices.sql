-- Lệnh SQL để tối ưu hóa hiệu năng truy vấn cho MySQL
-- Hãy chạy các lệnh này trong công cụ quản lý DB (như MySQL Workbench, HeidiSQL, hoặc CLI)

-- 1. Tối ưu hóa tìm kiếm sản phẩm theo tên và giá
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_price ON products(price);

-- 2. Tối ưu hóa truy vấn JOIN giữa sản phẩm với danh mục và thương hiệu
-- (Giúp tăng tốc độ cho @EntityGraph vừa được thêm vào)
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_brand_id ON products(brand_id);

-- 3. Tối ưu hóa tìm kiếm cho danh mục và thương hiệu
CREATE INDEX idx_categories_name ON categories(name);
CREATE INDEX idx_brands_name ON brands(name);

-- Xem các chỉ mục hiện có
-- SHOW INDEX FROM products;
