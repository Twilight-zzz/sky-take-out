package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
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

    //baidu地图相关
    @Value("${sky.shop.address}")
    private String shopAddress ;
    @Value("${sky.baidu.ak}")
    private String ak ;
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


        checkOutOfRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail()) ;
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

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenId() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = ordersMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);
    }

    /**
     * 用户端分页查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        PageHelper.startPage(ordersPageQueryDTO.getPage() , ordersPageQueryDTO.getPageSize());
        Page<Orders> page = ordersMapper.pageQuery(ordersPageQueryDTO) ;


        List<OrderVO> list = new ArrayList<>();
        if(page != null && !page.isEmpty()) {
            for(Orders order : page.getResult()){
                OrderVO orderVO = new OrderVO() ;
                BeanUtils.copyProperties(order , orderVO) ;
                //查询orderdetail
                Long orderId = order.getId() ;
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId) ;
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO) ;
            }
        }
        return new PageResult(page.getTotal() , list) ;
    }

    /**
     * 根据id查询订单详情
     * @param id 订单id
     * @return
     */
    public OrderVO getDetail(Long id){
        Orders order =  ordersMapper.getById(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO() ;
        BeanUtils.copyProperties(order , orderVO) ;
        orderVO.setOrderDetailList(orderDetails);
        return orderVO ;
    }

    /**
     * 取消订单
     * @param id
     */
    public void cancel(Long id) throws Exception{
        Orders orderDB = ordersMapper.getById(id);
        //先判断订单是否存在
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND) ;
        }
        Integer status = orderDB.getStatus();
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(status > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR) ;
        }
        Orders order = new Orders() ;
        order.setId(id);
        if(status.equals(Orders.TO_BE_CONFIRMED)){
            //此时需要退款
            weChatPayUtil.refund(order.getNumber(),order.getNumber(),new BigDecimal(0.01),new BigDecimal(0.01)) ;
            order.setPayStatus(Orders.REFUND);
        }
        order.setStatus(Orders.CANCELLED) ;
        order.setCancelTime(LocalDateTime.now()) ;
        order.setCancelReason("用户手动取消") ;

        ordersMapper.update(order) ;

    }

    /**
     * 再来一单
     * @param id 订单id
     */
    public void repetition(Long id){
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id) ;

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart() ;
            BeanUtils.copyProperties(orderDetail , shoppingCart , "id") ;
            shoppingCart.setUserId(userId) ;
            shoppingCart.setCreateTime(LocalDateTime.now()) ;
            return shoppingCart ;
        }).toList();
        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    /**
     * 管理端-订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        PageHelper.startPage(ordersPageQueryDTO.getPage() , ordersPageQueryDTO.getPageSize());
        Page<Orders> page = ordersMapper.pageQuery(ordersPageQueryDTO) ;
        //根据产品原型可知有时需要额外返回产品信息，所以统一返回OrderVO
        List<OrderVO> orderVOList = new ArrayList<>();

            for(Orders order : page.getResult()){
                OrderVO orderVO = new OrderVO() ;
                BeanUtils.copyProperties(order , orderVO) ;
                //获取orderdishes
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId()) ;
                List<String> orderDishList = orderDetailList.stream().map(x -> { String orderDish = x.getName()
                        + "*" + x.getNumber() ;
                    return orderDish ;
                }).toList() ;

                String orderDishes = orderDishList.isEmpty() ? "" : String.join("；" , orderDishList) + "；";
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO) ;
            }

        return new PageResult(page.getTotal() , orderVOList) ;
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics(){
        Integer toBeConfirmed = ordersMapper.countStatus(2) ;
        Integer confirmed = ordersMapper.countStatus(3) ;
        Integer deliveryInProgress = ordersMapper.countStatus(4) ;
        return new  OrderStatisticsVO(toBeConfirmed , confirmed , deliveryInProgress) ;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO){
        Orders order = Orders.builder().id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED).build();

        ordersMapper.update(order) ;
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception{
        //只有处于待接单是才可以拒单
        Orders orderDB = ordersMapper.getById(ordersRejectionDTO.getId()) ;

        if(orderDB == null || orderDB.getStatus() !=2 ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR) ;
        }
        //支付状态
        Integer payStatus = orderDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    orderDB.getNumber(),
                    orderDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        ordersMapper.update(orders);
    }

    /**
     * 管理端拒单
     * @param ordersCancelDTO
     * @throws Exception
     */
    public void cancel (OrdersCancelDTO ordersCancelDTO) throws Exception{
        Orders orderDB = ordersMapper.getById(ordersCancelDTO.getId()) ;
        //先判断订单是否存在
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND) ;
        }

        //若用户已经支付则要进行退款
        Integer payStatus = orderDB.getPayStatus();
        if(payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    orderDB.getNumber(),
                    orderDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        Orders order = Orders.builder().id(orderDB.getId())
                .status(Orders.CANCELLED).cancelReason(ordersCancelDTO.getCancelReason()).
                cancelTime(LocalDateTime.now()).build();

        ordersMapper.update(order) ;
    }

    /**
     * 派送订单
     * @param id
     */
    public void deliver(Long id){
        Orders orderDB = ordersMapper.getById(id) ;
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND) ;
        }
        if(! orderDB.getStatus() .equals(Orders.CONFIRMED) ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR) ;
        }
        Orders order = Orders.builder().id(id).status(Orders.DELIVERY_IN_PROGRESS).build();
        ordersMapper.update(order) ;
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = ordersMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        ordersMapper.update(orders);
    }


    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
