package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
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
        setmeal.setStatus(StatusConstant.DISABLE) ;
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

    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage() , setmealPageQueryDTO.getPageSize()) ;
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO) ;
        return new PageResult(page.getTotal() , page.getResult()) ;
    }

    /**
     * 批量删除
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids){
        ids.forEach(setmealId -> {
            Setmeal setmeal = setmealMapper.getById(setmealId) ;
            //起售中的套餐不能删除
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE) ;
            }

        }) ;

        ids.forEach(setmealId -> {
            setmealMapper.deleteById(setmealId) ;
            //同时删除setmealdish关联表中的数据
            setmealDishMapper.deleteBySetmealId(setmealId) ;
        });
    }
}
