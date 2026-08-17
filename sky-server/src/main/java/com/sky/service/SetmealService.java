package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids) ;

    /**
     * 根据id查询套餐VO
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id) ;

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void updateWithDish(SetmealDTO setmealDTO) ;


    /**
     * 起售或停售套餐
     * @param status
     * @param setmealId
     */
    void startOrStop(Integer status , Long setmealId) ;

    /**
     * 动态批量查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据套餐id查询对应菜品
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}
