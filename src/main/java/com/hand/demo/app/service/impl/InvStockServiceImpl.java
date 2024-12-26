package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvStockService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvStock;
import com.hand.demo.domain.repository.InvStockRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvStock)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:29:00
 */
@Service
public class InvStockServiceImpl implements InvStockService {
    @Autowired
    private InvStockRepository invStockRepository;

    @Override
    public Page<InvStock> selectList(PageRequest pageRequest, InvStock invStock) {
        return PageHelper.doPageAndSort(pageRequest, () -> invStockRepository.selectList(invStock));
    }

    @Override
    public void saveData(List<InvStock> invStocks) {
        List<InvStock> insertList = invStocks.stream().filter(line -> line.getStockId() == null).collect(Collectors.toList());
        List<InvStock> updateList = invStocks.stream().filter(line -> line.getStockId() != null).collect(Collectors.toList());
        invStockRepository.batchInsertSelective(insertList);
        invStockRepository.batchUpdateByPrimaryKeySelective(updateList);
    }
}

