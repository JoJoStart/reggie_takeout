package com.itheima.reggie.dto;

import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装页面提交的数据
 */
@Data
public class DishDto extends Dish {

    private List<DishFlavor> flavors = new ArrayList<DishFlavor>();

    private String categoryName;

    private Integer copies;
}
