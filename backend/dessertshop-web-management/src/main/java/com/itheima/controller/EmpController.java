package com.itheima.controller;

import com.itheima.mapper.EmpExprMapper;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequestMapping("/emps")
@RestController
public class EmpController {

    @Autowired
    private EmpService empService;
    @Autowired
    private EmpExprMapper empExprMapper;

   /* @GetMapping
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       String name, Integer gender,
                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ){
        log.info("查询请求参数,{},{},{},{},{},{}",page,pageSize,name,gender,begin,end);
        PageResult<Emp> pageResult = empService.page(page,pageSize,name,gender,begin,end);
        return Result.success(pageResult);
    }*/

    /**
     * 查询
     */
    @GetMapping
    public Result page(EmpQueryParam empQueryParam){
        log.info("查询请求参数:{}",empQueryParam);
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }
    /**
     * 新增
     */
    @PostMapping
    public Result save(@RequestBody Emp emp){
        log.info("新增员工:{}",emp);
        empService.save(emp);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping
    public Result delete(@RequestParam List<Integer> ids){
        log.info("批量删除部门:ids={}",ids);
        empService.deleteByIds(ids);
        return Result.success();
    }
    /**
     * 修改
     */

    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id){
        log.info("根据id查询员工的详细信息");
        Emp emp = empService.getinfo(id);
        return Result.success(emp);
    }

    @PutMapping
    public Result update(@RequestBody Emp emp){
        log.info("修改员工信息,{}",emp);
        empService.update(emp);
        return Result.success();
    }

    /**
     * 查询当前登录用户个人信息
     */
    @GetMapping("/profile/{id}")
    public Result getProfile(@PathVariable Integer id){
        log.info("查询用户个人信息,id:{}",id);
        Emp emp = empService.getProfile(id);
        if(emp != null){
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
                                  @RequestParam String newPassword){
        log.info("修改密码,id:{}",id);
        boolean success = empService.updatePassword(id, oldPassword, newPassword);
        if(success){
            return Result.success();
        }
        return Result.error("原密码错误");
    }
}
