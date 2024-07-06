package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPES;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() {
        String key = CACHE_SHOP_TYPES;
        //1.尝试获取redis里面的数据
        String shopTypesJson = stringRedisTemplate.opsForValue().get(key);
        //2.检验数据状态
        if(StrUtil.isNotBlank(shopTypesJson)){
            //3.redis存在直接返回
            List<ShopType> shopTypes = JSONUtil.toList(shopTypesJson, ShopType.class);
            return Result.ok(shopTypes);
        }
        //4.redis不存在，去数据库中查出来
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if(typeList.isEmpty()){
            return Result.fail("店铺类型查询失败");
        }
        //5.放入redis中
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList));
        return Result.ok(typeList);
    }
}
