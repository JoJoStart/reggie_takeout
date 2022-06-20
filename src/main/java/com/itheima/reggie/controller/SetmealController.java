package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    /**
     * 新增套餐
     *
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据name进行like模糊查询
        queryWrapper.like(name != null, Setmeal::getName, name);
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(pageInfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();

        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(item, setmealDto);
            //分类 id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                //分类名称
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    /**
     * 删除套餐功能
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids:{}", ids);

        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }

    /**
     * 停售/启售套餐（单个/批量）
     *
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status, @RequestParam List<Long> ids) {
        setmealService.status(status, ids);
        return R.success("售卖状态修改成功");
    }

    /**
     * 根据 id 查询套餐信息（包括套餐信息对应的菜品信息）
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getById(id);
        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息，同时更新对应的菜品信息
     *
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info(setmealDto.toString());

        if (setmealDto == null) {
            return R.error("请求异常");
        }

        if (setmealDto.getSetmealDishes() == null) {
            return R.error("套餐没有菜品,请添加套餐");
        }

        setmealService.updateWithDish(setmealDto);

        return R.success("修改菜品成功");
    }

    /**
     * 根据分类 id 查询对应的套餐信息
     *
     * @param setmeal
     * @return
     */
    @RequestMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<Setmeal>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 移动端点击套餐图片查看套餐具体内容
     * 这里返回的是 dto 对象，前端需要 copies 这个属性
     * 前端主要要展示的信息是：套餐中菜品的基本信息、图片、菜品描述、菜品的份数
     *
     * @param setmealId
     * @return
     */
    @GetMapping("/dish/{id}")
    public R<List<DishDto>> dish(@PathVariable("id") Long setmealId) {
        //构造查询条件
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<SetmealDish>();
        //添加查询条件
        queryWrapper.eq(SetmealDish::getSetmealId, setmealId);

        //获取套餐里面的所有菜品，即 SetmealDish 表里面的数据
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        List<DishDto> dishDtos = list.stream().map((setmealDish) -> {
            DishDto dishDto = new DishDto();
            //其实这个 BeanUtils 的拷贝是浅拷贝，这里要注意一下
            BeanUtils.copyProperties(setmealDish, dishDto);
            //这里是为了把套餐中的菜品的基本信息填充到 dto 中，比如菜品描述，菜品图片等菜品的基本信息
            Long dishId = setmealDish.getDishId();
            Dish dish = dishService.getById(dishId);
            BeanUtils.copyProperties(dish, dishDto);

            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtos);
    }

}
