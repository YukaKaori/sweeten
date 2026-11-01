package com.sweeten.inventory.service;

import com.sweeten.common.paging.PageResult;
import com.sweeten.inventory.model.Inventory;
import com.sweeten.inventory.model.InventoryQueryParam;

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
