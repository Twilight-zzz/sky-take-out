package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {


    @Autowired
    OrdersMapper ordersMapper;


    /**
     * 获取指定时间段的营业额
     * @param begin
     * @param end
     * @return
     */
    public TurnoverVO getTurnover(LocalDate begin, LocalDate end) {
        //构造出dateList和turnoverList即可
        List<LocalDate> dateList = new ArrayList();
        dateList.add(begin) ;
        while(!begin .equals(end) ){
            begin = begin.plusDays(1);
            dateList.add(begin) ;
        }
        //记得用string的join连接起来转成字符串集合就行

        //下面构造turnoverList
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime = LocalDateTime.of(date , LocalTime.MIN) ;
            LocalDateTime endTime = LocalDateTime.of(date , LocalTime.MAX) ;
            Map<String , Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status" , Orders.COMPLETED) ;
            Double turnover = ordersMapper.sumByMap(map) ;
            turnoverList.add(turnover) ;
        }

        //封装
        return TurnoverVO.builder()
                        .dateList(dateList.stream().map(String::valueOf)
                        .collect(Collectors.joining(",")))
                        .turnoverList(turnoverList.stream().map(String::valueOf)
                        .collect(Collectors.joining(",")))
                        .build() ;
}
}

