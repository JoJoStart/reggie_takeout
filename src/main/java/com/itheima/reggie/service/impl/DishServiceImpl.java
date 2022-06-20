package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    /**
     * 新增菜品，同时保存对应的口味数据
     *
     * @param dishDto
     */
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表 dish
        this.save(dishDto);

        Long dishId = dishDto.getId();//菜品 id

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表 dish_flavor
        dishFlavorService.saveBatch(flavors);

    }

    /**
     * 根据 id 查询菜品信息和对应的口味信息
     *
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从 dish 表查询
        Dish dish = this.getById(id);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        //查询当前菜品对应的口味信息，从 dish_flavor 表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<DishFlavor>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新对应的口味信息
     *
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新 dish 表基本信息
        this.updateById(dishDto);

        //清理当前菜品对应口味数据---dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据---dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 停售/启售单个菜品 或 批量停售/启售菜品
     *
     * @param status
     * @param ids
     */
    @Transactional
    public void status(Integer status, List<Long> ids) {
        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<Dish>();
        //添加条件
        queryWrapper.in(ids != null, Dish::getId, ids);

        List<Dish> list = dishService.list(queryWrapper);

        for (Dish dish : list) {
            if (dish != null) {
                dish.setStatus(status);
                dishService.updateById(dish);
            }
        }

    }

    /**
     * 根据 id 删除单个菜品或批量删除菜品
     *
     * @param ids
     */
    @Transactional
    public void deleteByIds(List<Long> ids) {
        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<Dish>();

        /* 查询该菜品的售卖状态。若为启售状态，则抛出业务异常；反之，则允许删除 */

        //添加条件
        queryWrapper.in(ids != null, Dish::getId, ids);

        List<Dish> list = this.list(queryWrapper);

        for (Dish dish : list) {
            Integer status = dish.getStatus();

            //若为停售状态，则可以删除
            if (status == 0) {
                this.removeById(dish.getId());
            } else {
                //此时应该回滚信息。防止出现恰好删除菜品时，该菜品也正在售卖中
                throw new CustomException("要删除的菜品中有正在售卖的菜品，无法全部删除");
            }
        }
    }
}
