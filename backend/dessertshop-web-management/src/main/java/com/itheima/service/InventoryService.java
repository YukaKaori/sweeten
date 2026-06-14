package com.itheima.service;

import com.itheima.pojo.Inventory;
import com.itheima.pojo.InventoryQueryParam;
import com.itheima.pojo.PageResult;

import java.util.List;

public interface InventoryService {

    PageResult<Inventory> page(InventoryQueryParam queryParam);

    Inventory getById(Integer id);

    void save(Inventory inventory);

    void update(Inventory inventory);

    void deleteById(Integer id);

    void adjustStock(Integer id, Integer quantity, String remark);

    List<Inventory> getAlerts();
}
