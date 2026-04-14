package ru.rt.rostelecom_tms.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        GenericJacksonJsonRedisSerializer typedValueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(60))
                .computePrefixWith(cacheName -> "v5::" + cacheName + "::")
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                typedValueSerializer
                        )
                );

        Map<String, RedisCacheConfiguration> customTtls = new HashMap<>();
        customTtls.put(CacheNames.DASHBOARD, defaults.entryTtl(Duration.ofSeconds(30)));
        customTtls.put(CacheNames.RUN_STATUSES, defaults.entryTtl(Duration.ofHours(12)));
        customTtls.put(CacheNames.ROLES, defaults.entryTtl(Duration.ofHours(12)));
        customTtls.put(CacheNames.PROJECTS_LIST, defaults.entryTtl(Duration.ofSeconds(60)));
        customTtls.put(CacheNames.PLANS_PAGE, defaults.entryTtl(Duration.ofSeconds(45)));
        customTtls.put(CacheNames.CASES_PAGE, defaults.entryTtl(Duration.ofSeconds(45)));
        customTtls.put(CacheNames.RUNS_PAGE, defaults.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(customTtls)
                .build();
    }

    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Fail-open: on cache deserialization/connection issues continue as cache miss.
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                // Fail-open: do not break request flow on cache write failure.
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Fail-open: cache eviction failure should not break business flow.
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                // Fail-open: cache clear failure should not break business flow.
            }
        };
    }
}
