local key = KEYS[1]
local window_size = tonumber(ARGV[1])
local max_requests = tonumber(ARGV[2])
local permits = tonumber(ARGV[3])

local current = redis.call('GET', key)
if current and tonumber(current) >= max_requests then
    return 0
else
    if not current then
        redis.call('SETEX', key, window_size, permits)
    else
        redis.call('INCRBY', key, permits)
    end
    return 1
end
