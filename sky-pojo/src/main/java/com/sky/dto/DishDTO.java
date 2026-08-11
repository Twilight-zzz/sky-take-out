package com.sky.dto;

import com.sky.entity.DishFlavor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DishDTO {
    private Long id ;

    private String name ;

    private Long categoryId ;

    private BigDecimal price ;

    private String image ;

    private String description ;

    private Integer status ;

    private List<DishFlavor> flavors = new ArrayList<>();
}
