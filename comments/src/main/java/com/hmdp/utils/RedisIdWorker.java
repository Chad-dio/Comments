package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RedisIdWorker {
    private final StringRedisTemplate stringRedisTemplate;

    private static final long BEGIN_TIMESTAMP = 1640995200L;

    private static final int COUNT_BITS = 32;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextID(String keyPrefix){
        //1.获取时间戳
        LocalDateTime now = LocalDateTime.now();
        long second = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = second - BEGIN_TIMESTAMP;
        //2.生成序列号
        //2.1按天统计，避免增长数量超过序列号
        String day = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.2自增
        long id = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + day, 1);
        //3.返回全局唯一ID
        return timeStamp << COUNT_BITS | id;
    }
}
