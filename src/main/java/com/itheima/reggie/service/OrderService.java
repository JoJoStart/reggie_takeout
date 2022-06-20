package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<Orders> {
    //用户下单
    public void submit(Orders orders);

    //根据订单 id 来得到一个订单明细的集合
    public List<OrderDetail> getOrderDetailListByOrderId(Long orderId);

    //用户查看自己的订单信息
    public void page(Page<Orders> ordersPageInfo, Page<OrdersDto> ordersDtoPageInfo);

    //再来一单
    public void againSubmit(@RequestBody Map<String, String> map);
}
