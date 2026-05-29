-- ============================================================
-- E2E 测试数据初始化脚本
-- 商户 id=1, 分类 id=2, 菜品 id=1 和 id=3
-- ============================================================

-- 商家 (id=1, status=1 营业中)
INSERT INTO `merchant` (`id`, `openid`, `name`, `phone`, `address`, `longitude`, `latitude`,
                        `description`, `business_hours`, `min_delivery`, `delivery_fee`,
                        `avg_delivery_time`, `monthly_sales`, `score`, `status`)
VALUES (1, 'wx_test_merchant_001', '张记川菜馆', '13900139000',
        '北京市朝阳区建国路88号', 116.461, 39.908,
        '正宗川菜，麻辣鲜香，十年老店', '09:00-22:00', 20.00, 5.00, 30, 1280, 4.8, 1);

-- 分类 (id=2, 关联 merchant_id=1)
INSERT INTO `category` (`id`, `merchant_id`, `name`, `sort`)
VALUES (2, 1, '招牌川菜', 1);

-- 菜品1 (id=1, merchant_id=1, category_id=2, status=1 上架)
INSERT INTO `dish` (`id`, `merchant_id`, `category_id`, `name`, `image`, `price`, `original_price`,
                    `summary`, `stock`, `monthly_sales`, `sort`, `status`)
VALUES (1, 1, 2, '麻辣小龙虾', 'https://example.com/dish1.png', 68.00, 88.00,
        '新鲜小龙虾，麻辣鲜香', 100, 520, 1, 1);

-- 菜品3 (id=3, merchant_id=1, category_id=2, status=1 上架)
INSERT INTO `dish` (`id`, `merchant_id`, `category_id`, `name`, `image`, `price`, `original_price`,
                    `summary`, `stock`, `monthly_sales`, `sort`, `status`)
VALUES (3, 1, 2, '水煮牛肉', 'https://example.com/dish3.png', 58.00, 68.00,
        '麻辣鲜香水煮牛肉，精选牛里脊', 100, 320, 2, 1);
