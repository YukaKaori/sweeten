package com.itheima.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 缓存配置
 * - 启用 Spring Cache
 * - JSON 序列化（支持 LocalDateTime 等 java.time 类型；多态类型仅允许本项目与 JDK 常用包）
 * - 不同缓存空间独立 TTL
 * - 事务感知：写库事务提交后才清除/写入缓存，避免并发请求把提交前的旧数据重新缓存
 * - Redis 不可用时快速降级：一次失败后熔断一段时间，期间直接走数据库，不再逐次等待连接超时；
 *   熔断期间漏掉的缓存清除会在 Redis 恢复后补做（整体清空），防止恢复后读到过期数据
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    /** Redis 失败后跳过缓存的时长 */
    private final Duration breakerOpenFor;
    /** 总开关：测试环境关闭，避免不同 H2 测试库之间经共享 Redis 串数据 */
    private final boolean cacheEnabled;

    public RedisConfig(@Value("${app.cache.breaker-open-seconds:30}") long breakerOpenSeconds,
                       @Value("${app.cache.enabled:true}") boolean cacheEnabled) {
        this.breakerOpenFor = Duration.ofSeconds(breakerOpenSeconds);
        this.cacheEnabled = cacheEnabled;
    }

    /** 缓存值序列化器：显式注册 JavaTimeModule（默认的 GenericJackson2JsonRedisSerializer 不支持 java.time，写入会全部失败） */
    static GenericJackson2JsonRedisSerializer jsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.itheima.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .build();
        mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer());
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        if (!cacheEnabled) {
            log.info("缓存已关闭(app.cache.enabled=false)，所有读写直查数据库");
            return new NoOpCacheManager();
        }
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()))
                .disableCachingNullValues();

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                // 甜品列表：10 分钟（修改后主动清除）
                .withCacheConfiguration("dessert:list",
                        defaultConfig.entryTtl(Duration.ofMinutes(10)))
                // Dashboard：5 分钟
                .withCacheConfiguration("dashboard:stats",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
        redisCacheManager.initializeCaches();
        return new FailFastCacheManager(redisCacheManager, breakerOpenFor);
    }

    /**
     * 缓存操作异常处理：仅记录日志，让请求穿透到数据库正常返回。
     * 实际的熔断与补偿清理在 FailFastCacheManager 中完成，这里只是兜底。
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("缓存读取失败，降级直查数据库 [cache={}, key={}]: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("缓存写入失败，忽略 [cache={}, key={}]: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("缓存清除失败，忽略 [cache={}, key={}]: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("缓存清空失败，忽略 [cache={}]: {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }

    // ==================== 熔断式缓存包装 ====================

    /**
     * 包装真实 CacheManager：任一缓存操作抛异常 → 熔断 breakerOpenFor，期间读=未命中、写/删=跳过（并记下"有漏删"）。
     * 熔断结束后的第一次缓存操作前，若有漏删则先把所有缓存清空，再恢复正常。
     * <p>
     * 包装顺序：TransactionAwareCacheDecorator(FailFastCache(RedisCache))。
     * 事务内的 put/evict 被推迟到提交后执行，推迟执行时同样经过 FailFastCache 的异常保护，
     * 不会出现"数据库已提交、却因 Redis 异常给前端返回 500"。
     */
    static class FailFastCacheManager implements CacheManager {
        private final CacheManager delegate;
        private final long openForMillis;
        private final AtomicLong openUntil = new AtomicLong(0);
        private final AtomicBoolean missedEvictions = new AtomicBoolean(false);
        private final ConcurrentMap<String, Cache> wrapped = new ConcurrentHashMap<>();

        FailFastCacheManager(CacheManager delegate, Duration openFor) {
            this.delegate = delegate;
            this.openForMillis = openFor.toMillis();
        }

        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            if (cache == null) {
                return null;
            }
            return wrapped.computeIfAbsent(name,
                    n -> new TransactionAwareCacheDecorator(new FailFastCache(cache, this)));
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }

        boolean isOpen() {
            return System.currentTimeMillis() < openUntil.get();
        }

        void trip(String op, String cacheName, RuntimeException e) {
            if (openUntil.getAndSet(System.currentTimeMillis() + openForMillis) < System.currentTimeMillis()) {
                log.warn("Redis 缓存不可用，{} 秒内跳过缓存直查数据库 [op={}, cache={}]: {}",
                        openForMillis / 1000, op, cacheName, e.getMessage());
            }
        }

        void markMissedEviction() {
            missedEvictions.set(true);
        }

        /** 熔断恢复后补做漏掉的清除；补做失败则再次熔断 */
        boolean recoverIfNeeded() {
            if (!missedEvictions.get()) {
                return true;
            }
            try {
                for (String name : delegate.getCacheNames()) {
                    Cache c = delegate.getCache(name);
                    if (c != null) {
                        c.clear();
                    }
                }
                missedEvictions.set(false);
                log.info("Redis 已恢复，已清空熔断期间可能过期的缓存");
                return true;
            } catch (RuntimeException e) {
                trip("recover-clear", "*", e);
                return false;
            }
        }
    }

    static class FailFastCache implements Cache {
        private final Cache delegate;
        private final FailFastCacheManager manager;

        FailFastCache(Cache delegate, FailFastCacheManager manager) {
            this.delegate = delegate;
            this.manager = manager;
        }

        private boolean usable() {
            return !manager.isOpen() && manager.recoverIfNeeded();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            if (!usable()) {
                return null;
            }
            try {
                return delegate.get(key);
            } catch (RuntimeException e) {
                manager.trip("get", getName(), e);
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            if (!usable()) {
                return null;
            }
            try {
                return delegate.get(key, type);
            } catch (RuntimeException e) {
                manager.trip("get", getName(), e);
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            if (usable()) {
                try {
                    return delegate.get(key, valueLoader);
                } catch (ValueRetrievalException e) {
                    throw e; // 业务方法本身抛的异常，原样抛出
                } catch (RuntimeException e) {
                    manager.trip("get", getName(), e);
                }
            }
            try {
                return valueLoader.call();
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            if (!usable()) {
                return;
            }
            try {
                delegate.put(key, value);
            } catch (RuntimeException e) {
                manager.trip("put", getName(), e);
            }
        }

        @Override
        public void evict(Object key) {
            if (!usable()) {
                manager.markMissedEviction();
                return;
            }
            try {
                delegate.evict(key);
            } catch (RuntimeException e) {
                manager.markMissedEviction();
                manager.trip("evict", getName(), e);
            }
        }

        @Override
        public void clear() {
            if (!usable()) {
                manager.markMissedEviction();
                return;
            }
            try {
                delegate.clear();
            } catch (RuntimeException e) {
                manager.markMissedEviction();
                manager.trip("clear", getName(), e);
            }
        }
    }
}
