package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 处理支付超时订单
     */
    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrder() {
        log.info("处理支付超时的订单: {}", LocalDateTime.now());
        //过15分种还未付款的订单自动取消
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> ordersList = ordersMapper.getByStatusAndOrdertime(Orders.PENDING_PAYMENT, time);
        if (ordersList != null && !ordersList.isEmpty()) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("支付超时，订单取消");
                ordersMapper.update(order);
            });
        }
    }


    /**
     * 处理“派送中”的订单
     * 每天凌晨一点执行这个任务，把前一天仍在“派送中”的订单设置为已完成
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("处理“派送中”的订单: {}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().minusMinutes(60);
        List<Orders> ordersList = ordersMapper.getByStatusAndOrdertime(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && !ordersList.isEmpty()) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                ordersMapper.update(order);
            });

        }
    }
}
