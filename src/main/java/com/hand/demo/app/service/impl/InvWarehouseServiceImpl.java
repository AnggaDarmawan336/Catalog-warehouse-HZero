package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvWarehouseService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvWarehouse;
import com.hand.demo.domain.repository.InvWarehouseRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvWarehouse)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:29:12
 */
@Service
public class InvWarehouseServiceImpl implements InvWarehouseService {
    @Autowired
    private InvWarehouseRepository invWarehouseRepository;

    @Override
    public Page<InvWarehouse> selectList(PageRequest pageRequest, InvWarehouse invWarehouse) {
        return PageHelper.doPageAndSort(pageRequest, () -> invWarehouseRepository.selectList(invWarehouse));
    }

    @Override
    public void saveData(List<InvWarehouse> invWarehouses) {
        List<InvWarehouse> insertList = invWarehouses.stream().filter(line -> line.getWarehouseId() == null).collect(Collectors.toList());
        List<InvWarehouse> updateList = invWarehouses.stream().filter(line -> line.getWarehouseId() != null).collect(Collectors.toList());
        invWarehouseRepository.batchInsertSelective(insertList);
        invWarehouseRepository.batchUpdateByPrimaryKeySelective(updateList);
    }
}

