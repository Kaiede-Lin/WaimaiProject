-- rollback_inventory.lua
-- KEYS[1] = inventory hash key, e.g. waimai:inventory:{merchantId}
-- ARGV[n] = dishId1, qty1, dishId2, qty2, ...

local hashKey = KEYS[1]

for i = 1, #ARGV, 2 do
    local dishId = ARGV[i]
    local qty = tonumber(ARGV[i + 1])
    redis.call('HINCRBY', hashKey, dishId, qty)
end

return 1
