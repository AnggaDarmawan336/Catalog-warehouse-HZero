package com.hand.demo.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import com.hand.demo.domain.entity.InvBatch;

import java.util.List;

/**
 * (InvBatch)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:27:55
 */
public interface InvBatchRepository extends BaseRepository<InvBatch> {
    /**
     * 查询
     *
     * @param invBatch 查询条件
     * @return 返回值
     */
    List<InvBatch> selectList(InvBatch invBatch);

    /**
     * 根据主键查询（可关联表）
     *
     * @param batchId 主键
     * @return 返回值
     */
    InvBatch selectByPrimary(Long batchId);
}
