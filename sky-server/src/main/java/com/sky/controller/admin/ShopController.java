package com.sky.controller.admin;


import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@Slf4j
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    public static final String KEY = "SHOP_STATUS" ;

    @Autowired
    RedisTemplate<String ,Object> redisTemplate;



    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status){

        log.info("设置店铺的营业状态为: {}" , status ==1 ? "营业中" : "打样中");
        redisTemplate.opsForValue().set(KEY , status) ;
        return Result.success();
    }


    @GetMapping("/status")
    @ApiOperation("管理端查询店铺状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY) ;
        if (status == null) {
            status = 0;
        }
        log.info("获取到店铺的营业状态为: {}" , status ==1 ? "营业中" : "打样中");
        return Result.success(status) ;

    }
}
