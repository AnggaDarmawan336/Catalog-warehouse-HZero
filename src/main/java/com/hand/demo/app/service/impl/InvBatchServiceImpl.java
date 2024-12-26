package com.hand.demo.app.service.impl;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvBatchService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvBatch;
import com.hand.demo.domain.repository.InvBatchRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (InvBatch)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:27:55
 */
@Service
public class InvBatchServiceImpl implements InvBatchService {
    @Autowired
    private InvBatchRepository invBatchRepository;

    @Override
    public Page<InvBatch> selectList(PageRequest pageRequest, InvBatch invBatch) {
        return PageHelper.doPageAndSort(pageRequest, () -> invBatchRepository.selectList(invBatch));
    }

    @Override
    public void saveData(List<InvBatch> invBatchs) {
        List<InvBatch> insertList = invBatchs.stream().filter(line -> line.getBatchId() == null).collect(Collectors.toList());
        List<InvBatch> updateList = invBatchs.stream().filter(line -> line.getBatchId() != null).collect(Collectors.toList());
        invBatchRepository.batchInsertSelective(insertList);
        invBatchRepository.batchUpdateByPrimaryKeySelective(updateList);
    }
}

