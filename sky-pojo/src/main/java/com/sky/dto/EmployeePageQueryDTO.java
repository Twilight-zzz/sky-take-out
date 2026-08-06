package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeePageQueryDTO {

    //员工姓名
    private String name ;

    //页码
    private int page ;

    //每页显示记录数
    private int pageSize ;
}
