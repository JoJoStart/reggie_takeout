package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     *
     * @param setmealDto
     */
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作 setmeal，执行 insert 操作
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //保存套餐和菜品的关联信息，操作 setmeal_dish，执行 insert 操作
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 删除套餐，同时需要删除套餐和菜品的关联数据
     *
     * @param ids
     */
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查询套餐状态，确定是否可用删除
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<Setmeal>();

        //sql 语句：select count(*) from setmeal where id in (1,2,3) and status =1;
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, 1);

        int count = this.count(queryWrapper);

        if (count > 0) {
            //如果不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        //如果可以删除，先删除套餐表中的数据 --- setmeal
        this.removeByIds(ids);

        //sql 语句：delete from setmeal_dish where setemal_id in (1,2,3)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<SetmealDish>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);

        //删除关系表中的数据
        setmealDishService.remove(lambdaQueryWrapper);
    }

    /**
     * 停售/启售套餐（单个/批量）
     *
     * @param status
     * @param ids
     */
    @Transactional
    public void status(Integer status, List<Long> ids) {
        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<Setmeal>();
        //添加条件
        queryWrapper.in(ids != null, Setmeal::getId, ids);

        List<Setmeal> list = this.list(queryWrapper);

        for (Setmeal setmeal : list) {
            if (setmeal != null) {
                setmeal.setStatus(status);
                this.updateById(setmeal);
            }
        }
    }

    /**
     * 根据 id 查询套餐信息
     *
     * @param id
     * @return
     */
    @Override
    public SetmealDto getById(Long id) {
        //查询套餐的基本信息，从 setmeal 表查询
        Setmeal setmeal = super.getById(id);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        //查询当前套餐对应的菜品。从关联表（setmeal_dish）中查询，
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<SetmealDish>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());

        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(list);
        return setmealDto;
    }

    /**
     * 更新套餐信息，同时更新对应的菜品信息
     *
     * @param setmealDto
     */
    @Override
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {
        //更新 setmeal 表的基本信息
        this.updateById(setmealDto);

        //清理当前套餐对应的菜品数据，即 setmeal_dish 表的 delete 操作
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<SetmealDish>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        //添加当前提交的菜品数据，即 setmeal_dish 表的 insert 操作
        List<SetmealDish> mealDishes = setmealDto.getSetmealDishes();

        mealDishes = mealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(mealDishes);
    }
}
