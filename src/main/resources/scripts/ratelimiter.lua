-- extract the key (same as argument, but relevant for sharding in clusters)
-- source: https://redis.io/docs/latest/develop/interact/programmability/eval-intro/#script-parameterization
local BUCKET_CAPACITY_FIELD_NAME = "cap"
local BUCKET_TIMESTAMP_FIELD_NAME = "ts"

local key = KEYS[1]

-- build the key for accessing the rate limit hash
local bucket_key = "ratelimit:" .. key

-- extract arguments - time of the request (unix epoch), the leak rate of buckets, and the maximum capacity
-- of buckets
local current_time = tonumber(ARGV[1])
local leak_rate = tonumber(ARGV[2])
local bucket_capacity = tonumber(ARGV[3])

-- fetch value of current capacity and the timestamp of the last bucket update for this user's bucket
local current_capacity = tonumber(redis.call("HGET", bucket_key, BUCKET_CAPACITY_FIELD_NAME) or "0")
local last_timestamp = tonumber(redis.call("HGET", bucket_key, BUCKET_TIMESTAMP_FIELD_NAME) or "0")

-- if this is the first time a request is being made for this user, there will be no
-- last_timestamp
if last_timestamp == 0 then
	last_timestamp = current_time
end

-- calculate how much time has passed since last request
local elapsed = current_time - last_timestamp

-- calculate how many "drops" have leaked since the last request, and use it to calculate
-- the new capacity of the bucket, but only update the timestamp if something leaked
local leaked = math.floor(elapsed * leak_rate)
if leaked > 0 then
	current_capacity = math.max(current_capacity - leaked, 0)
	last_timestamp = current_time
end

local allowed = false
if current_capacity < bucket_capacity then
	current_capacity = current_capacity + 1
	allowed = true
	last_timestamp = current_time
end

redis.call(
	"HSET",
	bucket_key,
	BUCKET_CAPACITY_FIELD_NAME,
	current_capacity,
	BUCKET_TIMESTAMP_FIELD_NAME,
	last_timestamp
)
-- set expire to the maximum amount of time before everything has leaked
redis.call("EXPIRE", bucket_key, math.floor(bucket_capacity / leak_rate) + 1)

return allowed
