package com.realm.config;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${SPRING_DATA_REDIS_HOST:${REDIS_HOST:localhost}}")
    private String redisHost;

    @Value("${SPRING_DATA_REDIS_PORT:${REDIS_PORT:6379}}")
    private int redisPort;

    @Value("${SPRING_DATA_REDIS_PASSWORD:${REDIS_PASSWORD:}}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000}")
    private long timeout;

    /**
     * Redis connection factory using Lettuce client with connection pooling
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Configure Redis connection
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        
        // Set password if provided
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        // Configure connection pool with timeout settings
        LettucePoolingClientConfiguration poolingConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .shutdownTimeout(Duration.ofMillis(200))
                .build();

        return new LettuceConnectionFactory(redisConfig, poolingConfig);
    }

    /**
     * Client resources for optimal Lettuce performance
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources lettuceClientResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(4)
                .computationThreadPoolSize(4)
                .build();
    }

    // RedisTemplate will be configured by SessionConfig for session management
}