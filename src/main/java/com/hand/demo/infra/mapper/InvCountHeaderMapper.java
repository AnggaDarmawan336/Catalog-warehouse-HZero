package com.hand.demo.infra.mapper;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.InvCountHeader;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (InvCountHeader)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:28:18
 */
public interface InvCountHeaderMapper extends BaseMapper<InvCountHeader> {
    /**
     * 基础查询
     *
     * @param invCountHeader 查询条件
     * @return 返回值
     */
    List<InvCountHeaderDTO> selectList(InvCountHeader invCountHeader);
}

