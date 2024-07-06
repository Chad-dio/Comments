package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Map;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

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

    @Override
    public Result queryById(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1.尝试获取redis里面的商户信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.检验redis获取状态
        if(StrUtil.isNotBlank(shopJson)){
            //3.获取成功，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //4.获取失败，需要去数据库查询
        Shop shop = getById(id);
        //5.数据库中不存在，返回获取错误
        if(shop == null){
            return Result.fail("该商户不存在");
        }
        //6.将查询到的数据存入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
        return Result.ok(shop);
    }
}
