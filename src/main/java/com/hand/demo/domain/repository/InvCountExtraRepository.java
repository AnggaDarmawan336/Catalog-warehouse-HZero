package com.hand.demo.domain.repository;

import org.hzero.mybatis.base.BaseRepository;
import com.hand.demo.domain.entity.InvCountExtra;

import java.util.List;

/**
 * (InvCountExtra)资源库
 *
 * @author Angga
 * @since 2024-12-17 14:28:07
 */
public interface InvCountExtraRepository extends BaseRepository<InvCountExtra> {
    /**
     * 查询
     *
     * @param invCountExtra 查询条件
     * @return 返回值
     */
    List<InvCountExtra> selectList(InvCountExtra invCountExtra);

    /**
     * 根据主键查询（可关联表）
     *
     * @param extraInfoId 主键
     * @return 返回值
     */
    InvCountExtra selectByPrimary(Long extraInfoId);
}
