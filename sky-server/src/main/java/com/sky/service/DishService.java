package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品
     * @param dishDTO
     */
    void save(DishDTO dishDTO) ;

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) ;

    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteBatch(List<Long> ids) ;

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    DishVO getById(Long id) ;

    /**
     * 修改菜品
     * @param dishDTO
     */
    void updateWithFlavor(DishDTO dishDTO) ;

    /**
     * 起售停售菜品
     * @param status
     * @param id
     */
    void startOrStop(Integer status , Long id) ;

    /**
     * 根据分类查询菜品
     * @param categoryId
     * @return
     */
    List<Dish> listByCategoryId(Long categoryId) ;
}
