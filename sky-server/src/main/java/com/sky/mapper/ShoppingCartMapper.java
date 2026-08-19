package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态批量查询
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);


    /**
     * 若购物车里已有相同商品则只更新商品数量
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart) ;


    /**
     * 向购物车中添加商品
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart (name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) " +
            " values (#{name},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{image},#{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 向购物车中批量添加商品
     * @param shoppingCartList
     */
    void insertBatch(@Param("shoppingCartList")List<ShoppingCart> shoppingCartList);


    /**
     * 根据用户id删除购物车数据
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId) ;
}
