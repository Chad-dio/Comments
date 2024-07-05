package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取客户端请求携带的token
        String token = request.getHeader("authorization");
        String key = RedisConstants.LOGIN_USER_KEY + token;
        //2.看redis里面存了该token对应的对象没
        Map<Object, Object> map = stringRedisTemplate.opsForHash()
                        .entries(key);
        //3.判断用户是否为空
        if (map.isEmpty()) {
            // 为空，需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        //4.提取用户信息
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        //5.存储用户
        UserHolder.saveUser(userDTO);
        //6.更新expire
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
