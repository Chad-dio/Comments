package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String name;

    private final static String prefix = "lock:";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String threadName = Thread.currentThread().getName();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(prefix + name, threadName, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(success);
    }

    @Override
    public void unLock() {
        String threadName = Thread.currentThread().getName();
        String lockValue = stringRedisTemplate.opsForValue().get(prefix + name);
        if (threadName.equals(lockValue)) {
            stringRedisTemplate.delete(prefix + name);
        }
    }
}
