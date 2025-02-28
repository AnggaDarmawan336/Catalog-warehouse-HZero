package com.hand.demo.infra.repository.impl;

import com.hand.demo.api.dto.InvStockDTO;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvStock;
import com.hand.demo.domain.repository.InvStockRepository;
import com.hand.demo.infra.mapper.InvStockMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (InvStock)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:29:00
 */
@Component
public class InvStockRepositoryImpl extends BaseRepositoryImpl<InvStock> implements InvStockRepository {
    @Resource
    private InvStockMapper invStockMapper;

    @Override
    public List<InvStock> selectList(InvStock invStock) {
        return invStockMapper.selectList(invStock);
    }

    @Override
    public InvStock selectByPrimary(Long stockId) {
        InvStock invStock = new InvStock();
        invStock.setStockId(stockId);
        List<InvStock> invStocks = invStockMapper.selectList(invStock);
        if (invStocks.size() == 0) {
            return null;
        }
        return invStocks.get(0);
    }

    @Override
    public List<InvStockDTO> selectSummary(InvStock invStock) {
        return invStockMapper.selectSummary(invStock);
    }

}

