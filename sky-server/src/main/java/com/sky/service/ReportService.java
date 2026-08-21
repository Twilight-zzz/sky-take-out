package com.sky.service;

import com.sky.vo.TurnoverVO;

import java.time.LocalDate;

public interface ReportService {


    /**
     * 获取指定时间段的营业额
     * @param begin
     * @param end
     * @return
     */
    TurnoverVO getTurnover(LocalDate begin, LocalDate end);
}
