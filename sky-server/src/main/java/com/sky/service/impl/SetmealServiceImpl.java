package com.sky.service.impl;

import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO , setmeal) ;
        setmealMapper.insert(setmeal) ;
        //接着根据插入表后获取的套餐id更新setmeal_dish这个表
        List<SetmealDish> list = setmealDTO.getSetmealDishes() ;
        Long setmealId = setmeal.getId() ;
        if(list != null && !list.isEmpty()){
            list.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId) ;
            });
            setmealDishMapper.insertBatch(list) ;
        }

    }
}
