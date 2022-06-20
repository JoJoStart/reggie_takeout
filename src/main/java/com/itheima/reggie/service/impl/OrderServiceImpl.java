package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import lombok.extern.slf4j.Slf4j;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.common.BaseContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.itheima.reggie.common.CustomException;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;


@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     *
     * @param orders
     */
    @Override
    public void submit(Orders orders) {
        //获得当前用户[id]
        Long userId = BaseContext.getCurrentId();

        //查询当前用户的[购物车]数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        //判断[购物车]是否为空
        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new CustomException("购物车为空，不可下单");
        }

        //查询[用户数据]
        User user = userService.getById(userId);

        //查询[地址数据]
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);

        //判断[地址信息]是否为空
        if (addressBook == null) {
            throw new CustomException("用户的地址簿信息有误，不可下单");
        }

        //使用 MyBatisPlus 提供的工具类生成的[订单号]
        long orderId = IdWorker.getId();

        //原子操作，保证线程安全
        AtomicInteger amount = new AtomicInteger(0);//使用原子类来保存计算的金额结果

        //进行[购物车]的金额数据计算，顺便把[订单]明细给计算出来
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());//菜品/套餐 的份数
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());//此处为单份的金额
            //[addAndGet]进行累加 [item.getAmount()]单份的金额  [multiply]乘  [item.getNumber()]份数
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        //填充订单的数据
        orders.setId(orderId);//对于 orders 表的 id，我们也可以使用订单号
        orders.setOrderTime(LocalDateTime.now());//下单时间
        orders.setCheckoutTime(LocalDateTime.now());//结单时间
        orders.setStatus(2);//订单状态（1.待付款，2.待派送，3.已派送，4.已完成，5.已取消）
        orders.setAmount(new BigDecimal(amount.get()));//订单的总金额
        orders.setUserId(userId);//下单用户的 id
        orders.setNumber(String.valueOf(orderId));//订单号
        orders.setUserName(user.getName());//下单用户的用户名
        orders.setConsignee(addressBook.getConsignee());//收货人
        orders.setPhone(addressBook.getPhone());//收货人手机号
        //拼接省市区 + 地址簿中的详细信息
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        //向[订单表]插入数据：一条数据
        this.save(orders);

        //向[订单明细表]插入数据：多条数据
        orderDetailService.saveBatch(orderDetails);

        //下单完成后，清空[购物车]数据
        shoppingCartService.remove(wrapper);
    }


    /**
     * 根据订单 id 来得到一个订单明细的集合
     *
     * @param orderId
     * @return
     */
    public List<OrderDetail> getOrderDetailListByOrderId(Long orderId) {
        /*此处为从[用户查看自己的订单信息]功能代码中抽离的一个方法
          旨在通过订单的 id 来查询订单明细，得到一个订单明细的集合

          单独抽离出来是为了避免在 stream 中遍历时
          直接使用的构造条件来查询导致 eq 叠加
          从而导致后面查询的数据都是 null*/
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);
        return orderDetailList;
    }

    /**
     * 用户查看自己的订单信息
     *
     * @param ordersPageInfo
     * @param ordersDtoPageInfo
     * @return
     */
    @Transactional
    public void page(Page<Orders> ordersPageInfo, Page<OrdersDto> ordersDtoPageInfo) {
        //构造条件查询对象
        LambdaQueryWrapper<Orders> queryWrapper_1 = new LambdaQueryWrapper<Orders>();
        //这里是直接把当前用户分页的全部结果查询出来，要添加用户 id 作为查询条件，否则会出现用户可以查询到其他用户的订单的情况
        queryWrapper_1.eq(Orders::getUserId, BaseContext.getCurrentId());
        //添加排序条件，根据更新时间降序排列
        queryWrapper_1.orderByDesc(Orders::getOrderTime);

        //分页查询（queryWrapper_1 -> ordersPageInfo）
        orderService.page(ordersPageInfo, queryWrapper_1);

        //通过 OrderId 查询对应的 OrderDetail
        //LambdaQueryWrapper<OrderDetail> queryWrapper_2 = new LambdaQueryWrapper<OrderDetail>();

        //对 OrderDto 进行必要的属性赋值
        List<Orders> records = ordersPageInfo.getRecords();
        List<OrdersDto> orderDtoList = records.stream().map((item) -> {
            OrdersDto orderDto = new OrdersDto();
            Long orderId = item.getId();//获取订单 id

            //通过订单的 id 来查询订单明细，得到一个订单明细的集合
            List<OrderDetail> orderDetailList = this.getOrderDetailListByOrderId(orderId);

            //为 orderDetails 里面的属性赋值
            BeanUtils.copyProperties(item, orderDto);

            //对 orderDto 进行 OrderDetails 属性的赋值
            orderDto.setOrderDetails(orderDetailList);

            return orderDto;
        }).collect(Collectors.toList());

        BeanUtils.copyProperties(ordersPageInfo, ordersDtoPageInfo, "records");
        ordersDtoPageInfo.setRecords(orderDtoList);
    }

    /**
     * 再来一单
     *
     * @param map
     */
    @Transactional
    public void againSubmit(@RequestBody Map<String, String> map) {
        /*
        前端点击"再来一单"，直接跳转到[index.html]，且购物车中是包含[菜品/套餐]信息的。
        为了避免数据有问题。再跳转之前我们需要把购物车的数据给清除。

        步骤：
            ① 通过 orderId 获取订单明细；
            ② 把订单明细的数据的数据塞到购物车表中。
                不过在此之前要先把购物车表中的数据给清除（清除的是当前登录用户的购物车表中的数据），不然就会导致再来一单的数据有问题。
                虽然这样可能会影响用户体验，但是就外卖而言，用户体验的影响不是很大。电商项目就不能这么干了。
        */
        String ids = map.get("id");
        long id = Long.parseLong(ids);
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, id);
        //获取该订单对应的所有的订单明细表
        //SQL 语句：SELECT id,name,order_id,dish_id,setmeal_id,dish_flavor,number,amount,image FROM order_detail WHERE (order_id = ?);
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);

        //通过用户 id 把原来的购物车给清空
        shoppingCartService.clean();//此处的 clean 方法在视频中出现过,建议抽取到 service 中以方便这里调用

        //获取用户 id
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map((item) -> {
            //把从 order 表中和 order_details 表中获取到的数据赋值给这个购物车对象
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            shoppingCart.setImage(item.getImage());
            Long dishId = item.getDishId();
            Long setmealId = item.getSetmealId();
            if (dishId != null) {
                //如果是菜品那就添加菜品的查询条件
                shoppingCart.setDishId(dishId);
            } else {
                //添加到购物车的是套餐
                shoppingCart.setSetmealId(setmealId);
            }
            shoppingCart.setName(item.getName());
            shoppingCart.setDishFlavor(item.getDishFlavor());
            shoppingCart.setNumber(item.getNumber());
            shoppingCart.setAmount(item.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        //把携带数据的购物车批量插入购物车表  这个批量保存的方法要使用熟练！！！
        shoppingCartService.saveBatch(shoppingCartList);
    }
}