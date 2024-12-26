package com.hand.demo.app.service;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvBatch;

import java.util.List;

/**
 * (InvBatch)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:27:55
 */
public interface InvBatchService {

    /**
     * 查询数据
     *
     * @param pageRequest 分页参数
     * @param invBatchs   查询条件
     * @return 返回值
     */
    Page<InvBatch> selectList(PageRequest pageRequest, InvBatch invBatchs);

    /**
     * 保存数据
     *
     * @param invBatchs 数据
     */
    void saveData(List<InvBatch> invBatchs);

}

