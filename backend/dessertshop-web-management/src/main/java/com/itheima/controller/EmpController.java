package com.itheima.controller;

import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/emps")
@RestController
public class EmpController {

    @Autowired
    private EmpService empService;

    /**
     * 分页查询员工
     */
    @GetMapping
    public Result page(EmpQueryParam empQueryParam) {
        log.info("查询请求参数:{}", empQueryParam);
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }

    /**
     * 新增员工
     */
    @PostMapping
    public Result save(@Valid @RequestBody Emp emp) {
        log.info("新增员工:{}", emp.getUsername());
        empService.save(emp);
        return Result.success();
    }

    /**
     * 批量删除员工
     */
    @DeleteMapping
    public Result delete(@RequestParam List<Integer> ids) {
        log.info("批量删除员工:ids={}", ids);
        empService.deleteByIds(ids);
        return Result.success();
    }

    /**
     * 根据ID查询员工详情
     */
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id) {
        log.info("根据id查询员工的详细信息,id:{}", id);
        Emp emp = empService.getinfo(id);
        return Result.success(emp);
    }

    /**
     * 修改员工信息
     */
    @PutMapping
    public Result update(@Valid @RequestBody Emp emp) {
        log.info("修改员工信息,username:{}", emp.getUsername());
        empService.update(emp);
        return Result.success();
    }

    /**
     * 查询当前登录用户个人信息
     */
    @GetMapping("/profile/{id}")
    public Result getProfile(@PathVariable Integer id) {
        log.info("查询用户个人信息,id:{}", id);
        Emp emp = empService.getProfile(id);
        if (emp != null) {
            emp.setPassword(null); // 不返回密码
        }
        return Result.success(emp);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result updatePassword(@RequestParam Integer id,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword) {
        log.info("修改密码,id:{}", id);
        boolean success = empService.updatePassword(id, oldPassword, newPassword);
        if (success) {
            return Result.success();
        }
        return Result.error("原密码错误");
    }
}
