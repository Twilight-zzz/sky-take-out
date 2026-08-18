package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    DishMapper dishMapper ;
    @Autowired
    DishFlavorMapper dishFlavorMapper ;
    @Autowired
    SetmealDishMapper setmealDishMapper ;
    @Autowired
    SetmealMapper setmealMapper ;

    /**
     * 新增菜品
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish() ;
        BeanUtils.copyProperties(dishDTO , dish) ;

        dishMapper.insert(dish) ;
        Long dishId = dish.getId() ;

        List<DishFlavor> flavors = dishDTO.getFlavors() ;
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId) ;
            });
        }
        //向口味表中插入n条数据
        dishFlavorMapper.insertBatch(flavors) ;

    }

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO){
        PageHelper.startPage(dishPageQueryDTO.getPage() , dishPageQueryDTO.getPageSize()) ;
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO) ;
        return new PageResult(page.getTotal() , page.getResult()) ;
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids){
        //先判断当前菜品是否起售
        for(Long id : ids){
            Dish dish = dishMapper.getById(id) ;
            if(dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids) ;
        if(setmealIds != null && setmealIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL) ;
        }

        //起首并且无关联套餐setmeal即可删除
        for(Long id : ids){
            dishMapper.deleteById(id) ;
            //记得删除关联的口味数据
            dishFlavorMapper.deleteByDishId(id) ;
        }
    }

    /**
     * 通过菜品id查询菜品
     * @param id
     * @return
     */
    public DishVO getById(Long id){
        Dish dish = dishMapper.getById(id) ;
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id) ;
        DishVO dishVO = new DishVO() ;
        BeanUtils.copyProperties(dish , dishVO) ;
        dishVO.setFlavors(flavors) ;
        return dishVO ;
    }

    /**
     * 修改菜品，注意同时也会修改菜品口味表
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish() ;
        BeanUtils.copyProperties(dishDTO, dish) ;
        dishMapper.update(dish) ;
        dishFlavorMapper.deleteByDishId(dish.getId()) ;
        List<DishFlavor> flavors = dishDTO.getFlavors() ;
        //不判空的话sql会报错，并且要设置dishId
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId()) ;
            });
            dishFlavorMapper.insertBatch(flavors) ;
        }
    }

    /**
     * 起售停售菜品
     * @param status
     * @param id
     */
    public void startOrStop(Integer status , Long id){
        Dish dish = Dish.builder().id(id).status(status).build() ;
        dishMapper.update(dish) ;

        //当一个菜品被停售之后要把相应套餐也停售
        if(status == StatusConstant.DISABLE){
            List<Long> dishIds = new ArrayList<>() ;
            dishIds.add(id) ;
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds) ;
            if(setmealIds != null && setmealIds.size() > 0){
                for(Long setmealId : setmealIds){
                    Setmeal setmeal = Setmeal.builder().id(setmealId).status(StatusConstant.DISABLE).build() ;
                    setmealMapper.update(setmeal) ;
                }
            }
        }
    }

    /**ESD
     * 根据分类动态查询所有菜品
     * @param categoryId
     * @return
     */
    public List<Dish> listByCategoryId(Long categoryId){
        Dish dish = Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build() ;
        return dishMapper.list(dish) ;
    }

    /**
     * 动态且批量返回DishVO
     * @param dish 自定义的dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish){
        List<Dish> dishList = dishMapper.list(dish) ;

        List<DishVO> dishVOList = new ArrayList<>() ;

        for(Dish d : dishList){
            DishVO dishVO = new DishVO() ;
            BeanUtils.copyProperties(d , dishVO) ;
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
            dishVO.setFlavors(flavors) ;
            dishVOList.add(dishVO) ;

        }

        return dishVOList ;
    }


}
