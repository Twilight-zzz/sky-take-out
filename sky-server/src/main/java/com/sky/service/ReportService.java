package com.sky.service;

import com.sky.vo.TurnoverVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {


    /**
     * 获取指定时间段的营业额
     * @param begin
     * @param end
     * @return
     */
    TurnoverVO getTurnover(LocalDate begin, LocalDate end);


    /**
     * 获取指定时间段的用户数据
     * @param begin
     * @param end
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);
}
