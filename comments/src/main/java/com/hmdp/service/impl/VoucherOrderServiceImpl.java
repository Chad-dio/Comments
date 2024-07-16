package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券信息
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        //2.检验优惠券信息合法
        //2.1没有优惠券
        if(voucher == null){
            return Result.fail("优惠券不存在");
        }
        //2.2时间不合法
        boolean f1 = LocalDateTime.now().isAfter(voucher.getBeginTime());
        boolean f2 = voucher.getEndTime().isBefore(LocalDateTime.now());
        if(f1 || f2){
            return Result.fail("优惠券购买时间不满足要求");
        }
        //2.3库存够不够
        int stock = voucher.getStock();
        if(stock < 1){
            return Result.fail("优惠券库存已经清空");
        }
        //3.购买合法，生成订单
        Long id = UserHolder.getUser().getId();
        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + id);
        boolean success = lock.tryLock(1200);
        if(!success){
            return Result.fail("不允许重复下单");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }finally {
            lock.unLock();
        }
    }

    @Override
    public Result createVoucherOrder(Long voucherId) {
        VoucherOrder order = new VoucherOrder();
        order.setVoucherId(voucherId);
        order.setId(UserHolder.getUser().getId());
        order.setCreateTime(LocalDateTime.now());
        int insert = baseMapper.insert(order);
        if(insert == 1){
            return Result.ok("创建订单成功");
        }
        return Result.fail("创建订单失败");
    }


}
