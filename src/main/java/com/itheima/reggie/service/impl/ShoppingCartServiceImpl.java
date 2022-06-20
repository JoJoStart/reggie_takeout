package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.mapper.ShoppingCartMapper;
import com.itheima.reggie.service.ShoppingCartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    /**
     * 清空购物车
     *
     * @return
     */
    @Transactional
    public R<String> clean() {
        //SQL：delete from shopping_cart where user_id = ?
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        this.remove(queryWrapper);
        return R.success("清空购物车成功");
    }
}
