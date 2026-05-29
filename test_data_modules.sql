-- ============================================================
-- Test Data: Sample coupons
-- ============================================================
USE waimai;

INSERT INTO coupon (name, type, threshold, discount_value, total_count, valid_days, status) VALUES
('新用户满20减5', 'FULL_REDUCTION', 20.00, 5.00, 1000, 30, 1),
('全场9折券', 'DISCOUNT', NULL, 10.00, 500, 7, 1),
('免配送费券', 'FREE_DELIVERY', NULL, 5.00, 200, 14, 1);
