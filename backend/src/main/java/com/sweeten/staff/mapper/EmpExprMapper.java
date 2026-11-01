package com.sweeten.staff.mapper;

import com.sweeten.staff.model.EmpExpr;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmpExprMapper {
    void insertBatch(List<EmpExpr> exprList);

    void deleteByEmpIds(@Param("empIds") List<Integer> ids);
}
