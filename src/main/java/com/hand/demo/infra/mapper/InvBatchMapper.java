package com.hand.demo.infra.mapper;

import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.InvBatch;

import java.util.List;

/**
 * (InvBatch)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:27:54
 */
public interface InvBatchMapper extends BaseMapper<InvBatch> {
    /**
     * 基础查询
     *
     * @param invBatch 查询条件
     * @return 返回值
     */
    List<InvBatch> selectList(InvBatch invBatch);
}

