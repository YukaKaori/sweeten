package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.EmpUpdateDTO;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.PasswordChangeRequest;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/emps")
@RestController
@Validated
@RequiredArgsConstructor
public class EmpController {

    private final EmpService empService;

    /**
     * 分页查询员工
     */
    @GetMapping
    public Result page(@Valid EmpQueryParam empQueryParam) {
        log.info("查询请求参数:{}", empQueryParam);
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }

    /**
     * 新增员工
     */
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Emp emp) {
        log.info("新增员工:{}", emp.getUsername());
        empService.save(emp);
        return Result.success();
    }

    /**
     * 批量删除员工
     */
    @LogOperation
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
    @LogOperation
    @PutMapping
    public Result update(@Valid @RequestBody EmpUpdateDTO empDTO) {
        log.info("修改员工信息,username:{}", empDTO.getUsername());
        // 转换为 Emp 对象，密码字段不传（updateById 的动态 SQL 会跳过 null 字段）
        Emp emp = new Emp();
        emp.setId(empDTO.getId());
        emp.setUsername(empDTO.getUsername());
        emp.setName(empDTO.getName());
        emp.setGender(empDTO.getGender());
        emp.setPhone(empDTO.getPhone());
        emp.setJob(empDTO.getJob());
        emp.setSalary(empDTO.getSalary());
        emp.setImage(empDTO.getImage());
        emp.setEntryDate(empDTO.getEntryDate());
        emp.setDeptId(empDTO.getDeptId());
        emp.setExprList(empDTO.getExprList());
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
     * 修改密码 — 使用请求体传输密码参数
     */
    @LogOperation
    @PutMapping("/password")
    public Result updatePassword(@RequestBody PasswordChangeRequest req) {
        log.info("修改密码,id:{}", req.getId());
        boolean success = empService.updatePassword(req.getId(), req.getOldPassword(), req.getNewPassword());
        if (success) {
            return Result.success();
        }
        return Result.error("原密码错误");
    }
}
