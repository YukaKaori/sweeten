package com.itheima.config;

import com.itheima.pojo.Dessert;
import com.itheima.pojo.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存层单元测试（不依赖真实 Redis）：序列化能处理 java.time；Redis 故障时熔断、恢复后补做漏掉的清除。
 */
@DisplayName("Redis 缓存序列化与故障降级")
class RedisCacheResilienceTest {

    @Test
    @DisplayName("序列化：含 LocalDateTime 的 PageResult<Dessert> 与 Map 能往返（原默认序列化器会写入失败）")
    void serializerRoundTripsJavaTime() {
        GenericJackson2JsonRedisSerializer serializer = RedisConfig.jsonSerializer();
        Dessert d = new Dessert();
        d.setId(1);
        d.setName("草莓蛋糕");
        d.setPrice(38.0);
        d.setCreateTime(LocalDateTime.of(2026, 10, 13, 9, 30));
        PageResult<Dessert> page = new PageResult<>(1L, new java.util.ArrayList<>(List.of(d)));

        Object back = serializer.deserialize(serializer.serialize(page));
        assertInstanceOf(PageResult.class, back);
        Dessert restored = (Dessert) ((PageResult<?>) back).getRows().get(0);
        assertEquals(d.getCreateTime(), restored.getCreateTime());
        assertEquals("草莓蛋糕", restored.getName());

        Map<String, Object> stats = new HashMap<>();
        stats.put("revenue", new java.math.BigDecimal("12.50"));
        stats.put("at", LocalDateTime.of(2026, 10, 13, 9, 30));
        assertEquals(stats, serializer.deserialize(serializer.serialize(stats)));
    }

    /** 可切换"故障"状态的缓存 */
    static class FlakyCache extends ConcurrentMapCache {
        final AtomicBoolean down = new AtomicBoolean(false);
        int calls;

        FlakyCache(String name) {
            super(name);
        }

        private void check() {
            calls++;
            if (down.get()) {
                throw new IllegalStateException("Unable to connect to Redis");
            }
        }

        @Override
        public ValueWrapper get(Object key) { check(); return super.get(key); }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) { check(); return super.get(key, valueLoader); }

        @Override
        public void put(Object key, Object value) { check(); super.put(key, value); }

        @Override
        public void evict(Object key) { check(); super.evict(key); }

        @Override
        public void clear() { check(); super.clear(); }
    }

    @Test
    @DisplayName("故障：一次失败即熔断，熔断期间不再访问 Redis；恢复后先清空漏删期间的旧数据")
    void breakerSkipsRedisAndReplaysMissedEvictions() throws Exception {
        FlakyCache flaky = new FlakyCache("dessert:list");
        SimpleCacheManager raw = new SimpleCacheManager();
        raw.setCaches(List.of(flaky));
        raw.initializeCaches();
        RedisConfig.FailFastCacheManager manager = new RedisConfig.FailFastCacheManager(raw, Duration.ofMillis(300));
        Cache cache = manager.getCache("dessert:list");

        cache.put("detail_1", "价格 38");
        assertEquals("价格 38", cache.get("detail_1").get());

        // Redis 故障：第一次读失败 → 当作未命中；之后熔断，不再访问
        flaky.down.set(true);
        assertNull(cache.get("detail_1"));
        int callsAfterTrip = flaky.calls;
        assertNull(cache.get("detail_1"));
        cache.evict("detail_1");          // 熔断期间改价：清除被跳过，但会被记下
        assertEquals(callsAfterTrip, flaky.calls, "熔断期间不应再访问 Redis");

        // Redis 恢复：旧值仍在 Redis 里，第一次访问前必须先整体清空
        flaky.down.set(false);
        Thread.sleep(350);
        assertNull(cache.get("detail_1"), "恢复后不能读到熔断期间应被清除的旧价格");
        cache.put("detail_1", "价格 40");
        assertEquals("价格 40", cache.get("detail_1").get());
    }

    @Test
    @DisplayName("故障：get(key, loader) 在 Redis 不可用时仍执行业务加载")
    void valueLoaderStillRunsWhenRedisDown() {
        FlakyCache flaky = new FlakyCache("dashboard:stats");
        flaky.down.set(true);
        SimpleCacheManager raw = new SimpleCacheManager();
        raw.setCaches(List.of(flaky));
        raw.initializeCaches();
        Cache cache = new RedisConfig.FailFastCacheManager(raw, Duration.ofSeconds(30)).getCache("dashboard:stats");

        assertEquals("from-db", cache.get("overview", () -> "from-db"));
    }
}
