package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id获得关联菜品的套餐的id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds) ;

    /**
     * 批量插入
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes) ;

    /**
     * 根据套餐id删除
     * @param id
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{id}")
    void deleteBySetmealId(Long id) ;

    /**
     * 根据套餐id查询
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getBySetmealId(Long id) ;


    /**
     * 统计一个套餐里关联了多少个状态为status的菜品
     * @param setmealId
     * @param status
     * @return
     */
    @Select("""
select count(*) 
from dish d
inner join setmeal_dish sd on d.id = sd.dish_id 
where sd.setmeal_id = #{setmealId} 
and d.status = #{status}
""")
    Integer countBySetmealIdAndDishStatus(Long setmealId , Integer status) ;
}
