package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 提交订单
     * @param ordersSubmitDTO
     * @return
     */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //先判断异常
        //1.收货地址为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId()) ;
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //2.购物车为空
        Long userId = BaseContext.getCurrentId() ;
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart) ;
        if(shoppingCartList == null || shoppingCartList.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL) ;
        }

        //接下来构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);

        //设置属性
        //order.setNumber(String.valueOf(System.currentTimeMillis()));
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) ;
        //生成当天流水号
        Long serial =stringRedisTemplate.opsForValue().increment("order:" + date) ;
        order.setNumber(date + String.format("%06d",serial) ) ;

        order.setStatus(Orders.PENDING_PAYMENT);
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID) ;
        //order.setUserName()
        order.setPhone(addressBook.getPhone()) ;
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee()) ;

        ordersMapper.insert(order) ;

        //接下来再往订单明细表order_detail里插入数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail() ;
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(order.getId()) ;
            orderDetailList.add(orderDetail) ;
        }
        orderDetailMapper.insertBatch(orderDetailList) ;

        shoppingCartMapper.deleteByUserId(userId) ;

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().id(order.getId()).orderNumber(order.getNumber())
                .orderAmount(order.getAmount()).orderTime(order.getOrderTime()).build();
        return orderSubmitVO ;

    }
}
