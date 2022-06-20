package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import org.springframework.web.bind.annotation.RequestBody;

public interface ShoppingCartService extends IService<ShoppingCart> {
    //清空购物车
    public R<String> clean();
}
