package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

@RestController("adminDishController")
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    DishService dishService ;

    @Autowired
    RedisTemplate redisTemplate ;

    /**
     * 清理缓存数据
     * @param pattern key的模式
     */
    private void cleanCache(String pattern){
        Set<String> keys = redisTemplate.keys(pattern) ;
        redisTemplate.delete(keys) ;
    }

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品： {}" , dishDTO) ;
        dishService.saveWithFlavor(dishDTO) ;

        //清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId() ;
        cleanCache(key) ;
        return Result.success() ;
    }

    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询菜品: {}" , dishPageQueryDTO) ;
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO) ;
        return Result.success(pageResult) ;
    }

    @DeleteMapping()
    @ApiOperation("删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除菜品：{}" , ids) ;
        dishService.deleteBatch(ids) ;


        cleanCache("dish_*") ;
        return Result.success() ;
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品: {}" , id) ;
        DishVO dishVO = dishService.getById(id) ;
        return Result.success(dishVO) ;
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品 ： {}" , dishDTO) ;
        dishService.updateWithFlavor(dishDTO) ;

        cleanCache("dish_*") ;
        return Result.success() ;

    }

    @PostMapping("/status/{status}")
    @ApiOperation("起售停售菜品")
    public Result startOrStop(@PathVariable Integer status , @RequestParam Long id){
        log.info("起售停售菜品: {} , {}" , status, id) ;
        dishService.startOrStop(status , id) ;

        cleanCache("dish_*") ;
        return Result.success() ;
    }

    @GetMapping("/list")
    @ApiOperation("根据categoryId查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.listByCategoryId(categoryId);
        return Result.success(list) ;
    }
}
