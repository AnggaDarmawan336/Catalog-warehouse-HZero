package com.hand.demo.infra.mapper;

import com.hand.demo.api.dto.InvStockDTO;
import io.choerodon.mybatis.common.BaseMapper;
import com.hand.demo.domain.entity.InvStock;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * (InvStock)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:29:00
 */
public interface InvStockMapper extends BaseMapper<InvStock> {
    /**
     * 基础查询
     *
     * @param invStock 查询条件
     * @return 返回值
     */
    List<InvStock> selectList(InvStock invStock);

    List<InvStockDTO> selectSummary(InvStock invStock);

}

