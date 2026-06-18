package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.EmpUpdateDTO;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.PasswordChangeRequest;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "03-员工管理", description = "员工信息的增删改查与个人信息管理接口")
public class EmpController {

    private final EmpService empService;

    @Operation(summary = "分页查询员工", description = "支持按姓名、性别、部门、职位、入职日期范围等条件分页查询员工列表")
    @GetMapping
    public Result page(@Valid EmpQueryParam empQueryParam) {
        log.info("查询请求参数:{}", empQueryParam);
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }

    @Operation(summary = "新增员工", description = "新增一个员工，包含基本信息和工作经历")
    @LogOperation
    @PostMapping
    public Result save(@Valid @RequestBody Emp emp) {
        log.info("新增员工:{}", emp.getUsername());
        empService.save(emp);
        return Result.success();
    }

    @Operation(summary = "批量删除员工", description = "根据员工ID列表批量删除员工")
    @LogOperation
    @DeleteMapping
    public Result delete(@Parameter(description = "员工ID列表") @RequestParam List<Integer> ids) {
        log.info("批量删除员工:ids={}", ids);
        empService.deleteByIds(ids);
        return Result.success();
    }

    @Operation(summary = "根据ID查询员工详情", description = "根据员工ID查询员工的详细信息，包含工作经历")
    @GetMapping("/{id}")
    public Result getInfo(@Parameter(description = "员工ID") @PathVariable Integer id) {
        log.info("根据id查询员工的详细信息,id:{}", id);
        Emp emp = empService.getinfo(id);
        return Result.success(emp);
    }

    @Operation(summary = "修改员工信息", description = "修改员工的基本信息和工作经历")
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

    @Operation(summary = "查询当前登录用户个人信息", description = "根据用户ID获取当前登录用户的个人信息")
    @GetMapping("/profile/{id}")
    public Result getProfile(@Parameter(description = "用户ID") @PathVariable Integer id) {
        log.info("查询用户个人信息,id:{}", id);
        Emp emp = empService.getProfile(id);
        if (emp != null) {
            emp.setPassword(null); // 不返回密码
        }
        return Result.success(emp);
    }

    @Operation(summary = "修改密码", description = "修改当前登录用户的密码，需要验证原密码")
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
