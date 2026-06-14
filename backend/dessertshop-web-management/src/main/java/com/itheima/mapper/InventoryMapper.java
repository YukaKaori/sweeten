package com.itheima.mapper;

import com.itheima.pojo.Inventory;
import com.itheima.pojo.InventoryQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InventoryMapper {

    List<Inventory> list(InventoryQueryParam queryParam);

    @Select("select * from inventory where id = #{id}")
    Inventory getById(Integer id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into inventory(name, category, stock, unit, safety_threshold, supplier, expiry_date, remark, create_time, update_time) " +
            "values(#{name}, #{category}, #{stock}, #{unit}, #{safetyThreshold}, #{supplier}, #{expiryDate}, #{remark}, #{createTime}, #{updateTime})")
    void insert(Inventory inventory);

    void updateById(Inventory inventory);

    @Delete("delete from inventory where id = #{id}")
    void deleteById(Integer id);

    @Update("update inventory set stock = stock + #{quantity}, update_time = now() where id = #{id}")
    void adjustStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    @Select("select * from inventory where stock <= safety_threshold order by stock asc")
    List<Inventory> selectAlerts();
}
