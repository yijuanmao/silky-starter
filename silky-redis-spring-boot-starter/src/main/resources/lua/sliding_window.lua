local key = KEYS[1]
local window_size = tonumber(ARGV[1])
local max_requests = tonumber(ARGV[2])
local permits = tonumber(ARGV[3])
local now = tonumber(ARGV[4])
local clear_before = now - window_size

redis.call('ZREMRANGEBYSCORE', key, 0, clear_before)
local current = redis.call('ZCOUNT', key, clear_before + 1, now)

if current + permits > max_requests then
    return 0
else
    for i = 1, permits do
        redis.call('ZADD', key, now, now .. ':' .. math.random(1000000))
    end
    redis.call('EXPIRE', key, window_size)
    return 1
end
