-- Fix garbled Chinese characters in system_config table
-- Run this SQL in your MySQL client if the descriptions show garbled text

USE waimai;

UPDATE system_config SET description = '默认配送费(元)' WHERE config_key = 'delivery_fee_default';
UPDATE system_config SET description = '未支付自动取消时间(分钟)' WHERE config_key = 'auto_cancel_minutes';
UPDATE system_config SET description = '最大配送半径(公里)' WHERE config_key = 'max_delivery_radius';
UPDATE system_config SET description = '骑手每单基础收入(元)' WHERE config_key = 'rider_income_per_order';

-- Verify
SELECT * FROM system_config;
