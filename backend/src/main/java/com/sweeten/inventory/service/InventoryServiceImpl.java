package com.sweeten.inventory.service;

import com.sweeten.common.paging.PageHelperUtils;
import com.sweeten.common.paging.PageResult;
import com.sweeten.inventory.mapper.InventoryMapper;
import com.sweeten.inventory.model.Inventory;
import com.sweeten.inventory.model.InventoryQueryParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    @Override
    public PageResult<Inventory> page(InventoryQueryParam queryParam) {
        return PageHelperUtils.executePage(queryParam.getPage(), queryParam.getPageSize(),
                () -> inventoryMapper.list(queryParam));
    }

    @Override
    public Inventory getById(Integer id) {
        return inventoryMapper.getById(id);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Inventory inventory) {
        inventory.setCreateTime(LocalDateTime.now());
        inventory.setUpdateTime(LocalDateTime.now());
        inventoryMapper.insert(inventory);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Inventory inventory) {
        inventory.setUpdateTime(LocalDateTime.now());
        inventoryMapper.updateById(inventory);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void deleteById(Integer id) {
        inventoryMapper.deleteById(id);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void adjustStock(Integer id, Integer quantity, String remark) {
        inventoryMapper.adjustStock(id, quantity);
    }

    @Override
    public List<Inventory> getAlerts() {
        return inventoryMapper.selectAlerts();
    }
}
