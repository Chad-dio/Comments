package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        return queryByIdByMutex(id);
    }

    private Result queryByIdByMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        //1.尝试获取redis里面的商户信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.检验redis获取状态
        if(StrUtil.isNotBlank(shopJson)){
            //3.获取店铺信息成功，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //4.redis里面不存在，需要重建
        //4.1第一步尝试获取锁
        Shop shop;
        String lock = LOCK_SHOP_KEY + id;
        try {
            boolean flag = tryLock(lock);
            if(!flag){
                Thread.sleep(50);
                return queryByIdByMutex(id);
            }
            //4.2成功拿到锁，判断自己是第一个拿到锁，还是redis缓存重建完成了
            shopJson = stringRedisTemplate.opsForValue().get(key);
            //redis缓存重建完成
            if(StrUtil.isNotBlank(shopJson)){
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return Result.ok(shop);
            }
            //5.去数据库查询，完成缓存的重建
            shop = getById(id);
            //6.数据库中不存在，返回获取错误
            if(shop == null){
                return Result.fail("该商户不存在");
            }
            //7.将查询到的数据存入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //8.释放锁
            delLock(lock);
        }
        return Result.ok(shop);
    }

    private Result queryByIdByThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1.尝试获取redis里面的商户信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.检验redis获取状态
        if(StrUtil.isNotBlank(shopJson)){
            //3.获取店铺信息成功，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //4.获取的是缓存的null值，代表请求的id错误的
        if(shopJson == null){
            return Result.fail("该商户不存在");
        }
        //5.获取失败，需要去数据库查询
        Shop shop = getById(id);
        //6.数据库中不存在，返回获取错误,并向redis中缓存空值
        if(shop == null){
            stringRedisTemplate.opsForValue().set(key, "", CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.fail("该商户不存在");
        }
        //7.将查询到的数据存入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }


    private boolean tryLock(String key){
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(lock);
    }

    private void delLock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return  Result.fail("商户ID不能为空");
        }
        //1.修改数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
