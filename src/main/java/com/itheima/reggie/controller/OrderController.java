package com.itheima.reggie.controller;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrderService;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     *
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("订单数据：{}", orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 用户端展示自己的订单分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> page(int page, int pageSize) {
        //分页构造器对象
        Page<Orders> ordersPageInfo = new Page<Orders>(page, pageSize);
        Page<OrdersDto> ordersDtoPageInfo = new Page<OrdersDto>(page, pageSize);
        orderService.page(ordersPageInfo, ordersDtoPageInfo);
        return R.success(ordersDtoPageInfo);
    }

    /**
     * 再来一单
     *
     * @param order_1
     * @return
     */
    /*@PostMapping("/again")
    @Transactional
    public R<String> again(@RequestBody Orders order_1) {
        log.info("再来一单_数据测试：{}", order_1);

        //取得[订单id]
        Long id = order_1.getId();
        //通过[订单id]获得相应的[order]对象
        Orders orders = orderService.getById(id);

        //在[order]对象中重新设置[订单id]
        long orderId = IdWorker.getId();
        orders.setId(orderId);

        //在[order]对象中重新设置[订单号码]
        String number = String.valueOf(IdWorker.getId());
        orders.setNumber(number);

        //在[order]对象中重新设置[下单时间]
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);

        //最终，将这条数据插入订单表（orders）
        orderService.save(orders);

        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<OrderDetail>();
        //设置查询条件
        queryWrapper.eq(OrderDetail::getOrderId, id);
        List<OrderDetail> list = orderDetailService.list(queryWrapper);

        list.stream().map((item) -> {
            //订单明细表 id
            long detailId = IdWorker.getId();
            //设置[订单号码]
            item.setOrderId(orderId);
            item.setId(detailId);
            return item;
        }).collect(Collectors.toList());

        //向 订单明细表 中插入多条数据
        orderDetailService.saveBatch(list);
        return R.success("操作成功");
    }*/


    /**
     * 再来一单
     *
     * @param map
     * @return
     */
    @PostMapping("/again")
    public R<String> againSubmit(@RequestBody Map<String, String> map) {
        orderService.againSubmit(map);
        return R.success("操作成功");
    }

    /**
     * 订单的分页查询
     *
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {
        //构造分页构造器
        Page<Orders> ordersPageInfo = new Page<>(page, pageSize);
        Page<OrdersDto> ordersDtoPageInfo = new Page<>();

        //构造条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //根据 number 进行模糊查询
        queryWrapper.like(!StringUtils.isEmpty(number), Orders::getNumber, number);
        //根据 Datetime 进行时间范围查询
        if (beginTime != null && endTime != null) {
            queryWrapper.ge(Orders::getOrderTime, beginTime);//ge ：大于等于 >=
            queryWrapper.le(Orders::getOrderTime, endTime);//le ：小于等于 <=
        }

        //添加排序条件
        queryWrapper.orderByDesc(Orders::getOrderTime);

        //进行分页查询
        orderService.page(ordersPageInfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(ordersPageInfo, ordersDtoPageInfo, "records");

        List<Orders> records = ordersPageInfo.getRecords();

        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            String consignee = item.getConsignee();
            String name = consignee + "_" + item.getUserId();
            ordersDto.setUserName(name);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPageInfo.setRecords(list);
        return R.success(ordersDtoPageInfo);
    }

    /**
     * 后台修改订单状态
     *
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> status(@RequestBody Orders orders) {
        Long orderId = orders.getId();
        Integer status = orders.getStatus();

        if(orderId == null || status==null){
            return R.error("传入信息不合法");
        }

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getId, orderId);
        Orders one = orderService.getOne(queryWrapper);
        one.setStatus(status);

        orderService.updateById(one);
        return R.success("派送成功");
    }

}