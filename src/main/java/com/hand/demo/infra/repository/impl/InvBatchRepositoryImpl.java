package com.hand.demo.infra.repository.impl;

import org.apache.commons.collections.CollectionUtils;
import org.hzero.mybatis.base.impl.BaseRepositoryImpl;
import org.springframework.stereotype.Component;
import com.hand.demo.domain.entity.InvBatch;
import com.hand.demo.domain.repository.InvBatchRepository;
import com.hand.demo.infra.mapper.InvBatchMapper;

import javax.annotation.Resource;
import java.util.List;

/**
 * (InvBatch)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:27:55
 */
@Component
public class InvBatchRepositoryImpl extends BaseRepositoryImpl<InvBatch> implements InvBatchRepository {
    @Resource
    private InvBatchMapper invBatchMapper;

    @Override
    public List<InvBatch> selectList(InvBatch invBatch) {
        return invBatchMapper.selectList(invBatch);
    }

    @Override
    public InvBatch selectByPrimary(Long batchId) {
        InvBatch invBatch = new InvBatch();
        invBatch.setBatchId(batchId);
        List<InvBatch> invBatchs = invBatchMapper.selectList(invBatch);
        if (invBatchs.size() == 0) {
            return null;
        }
        return invBatchs.get(0);
    }

}

