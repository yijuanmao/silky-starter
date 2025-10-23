local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local refill_time_unit = tonumber(ARGV[3])
local permits = tonumber(ARGV[4])
local now = tonumber(ARGV[5])

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill_time')
local tokens = capacity
local last_refill_time = now

if bucket[1] then
    tokens = tonumber(bucket[1])
    last_refill_time = tonumber(bucket[2])

    -- 计算需要补充的令牌数
    local time_passed = now - last_refill_time
    local tokens_to_add = math.floor(time_passed * refill_rate / refill_time_unit)

    if tokens_to_add > 0 then
        tokens = math.min(capacity, tokens + tokens_to_add)
        last_refill_time = now
    end
end

if tokens >= permits then
    tokens = tokens - permits
    redis.call('HMSET', key, 'tokens', tokens, 'last_refill_time', last_refill_time)
    redis.call('EXPIRE', key, math.ceil(capacity / refill_rate * refill_time_unit) * 2)
    return 1
else
    return 0
end
