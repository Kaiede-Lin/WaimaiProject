-- deduct_inventory.lua
-- KEYS[1] = inventory hash key, e.g. waimai:inventory:{merchantId}
-- ARGV[n] = dishId1, qty1, dishId2, qty2, ...
-- Returns: 0 = success, 1 = stock insufficient, 2 = dish not found

local hashKey = KEYS[1]

for i = 1, #ARGV, 2 do
    local dishId = ARGV[i]
    local qty = tonumber(ARGV[i + 1])
    local stock = redis.call('HGET', hashKey, dishId)
    if stock == false then
        return {2, dishId, 0}
    end
    stock = tonumber(stock)
    if stock < qty then
        return {1, dishId, stock}
    end
end

for i = 1, #ARGV, 2 do
    local dishId = ARGV[i]
    local qty = tonumber(ARGV[i + 1])
    redis.call('HINCRBY', hashKey, dishId, -qty)
end

return {0}
