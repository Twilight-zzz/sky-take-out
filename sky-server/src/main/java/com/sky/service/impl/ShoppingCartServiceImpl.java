package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    ShoppingCartMapper shoppingCartMapper;

    @Autowired
    DishMapper dishMapper;

    @Autowired
    SetmealMapper setmealMapper;

    public void add(ShoppingCartDTO shoppingCartDTO){
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        //先绑定到对应用户上
        shoppingCart.setUserId(BaseContext.getCurrentId()) ;
        //先判断当前商品是否在购物车中
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart) ;
        if(shoppingCartList!=null&&shoppingCartList.size() == 1){
            shoppingCart = shoppingCartList.get(0) ;
            shoppingCart.setNumber(shoppingCart.getNumber()+1);
            shoppingCartMapper.updateNumberById(shoppingCart) ;
        }

        else{
            Long dishId = shoppingCartDTO.getDishId();
            //如果不在，那么就要调用insert插入数据，分成dish和setmeal两类讨论即可
            //1.插入的是菜品
            if(dishId != null){
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage()) ;
                shoppingCart.setAmount(dish.getPrice()) ;
            }
            //2.插入的是套餐
            else{
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage()) ;
                shoppingCart.setAmount(setmeal.getPrice()) ;
            }

            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            //最后调用insert插入
            shoppingCartMapper.insert(shoppingCart) ;
        }


    }


    /**
     * 展示购物车中所有内容
     * @return
     */
    public List<ShoppingCart> showShoppingCart(){
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        return shoppingCartMapper.list(shoppingCart);

    }
}
