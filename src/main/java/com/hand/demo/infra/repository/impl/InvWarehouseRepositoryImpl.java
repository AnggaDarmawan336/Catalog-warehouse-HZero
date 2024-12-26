package com.hand.demo.infra.repository.impl;

import org.apache.commons.collections.CollectionUtils;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvWarehouse;
import com.hand.demo.domain.repository.InvWarehouseRepository;
import com.hand.demo.infra.mapper.InvWarehouseMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (InvWarehouse)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:29:12
 */
@Component
public class InvWarehouseRepositoryImpl extends BaseRepositoryImpl<InvWarehouse> implements InvWarehouseRepository {
    @Resource
    private InvWarehouseMapper invWarehouseMapper;

    @Override
    public List<InvWarehouse> selectList(InvWarehouse invWarehouse) {
        return invWarehouseMapper.selectList(invWarehouse);
    }

    @Override
    public InvWarehouse selectByPrimary(Long warehouseId) {
        InvWarehouse invWarehouse = new InvWarehouse();
        invWarehouse.setWarehouseId(warehouseId);
        List<InvWarehouse> invWarehouses = invWarehouseMapper.selectList(invWarehouse);
        if (invWarehouses.size() == 0) {
            return null;
        }
        return invWarehouses.get(0);
    }

}

