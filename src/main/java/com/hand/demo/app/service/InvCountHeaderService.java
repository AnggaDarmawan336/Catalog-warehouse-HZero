package com.hand.demo.app.service;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import com.hand.demo.api.dto.WorkFlowDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import com.hand.demo.domain.entity.InvCountHeader;

import java.util.List;

/**
 * (InvCountHeader)应用服务
 *
 * @author Angga
 * @since 2024-12-17 14:28:19
 */
public interface InvCountHeaderService {

    /**
     * 查询数据
     *
     * @param pageRequest     分页参数
     * @param invCountHeaders 查询条件
     * @return 返回值
     */
    Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeaderDTO invCountHeaders);

    /**
     * 保存数据
     *
     * @param invCountHeaderDTO 数据
     */
    List<InvCountHeaderDTO> orderSave(List<InvCountHeaderDTO> invCountHeaderDTO);

    InvCountInfoDTO manualSaveCheck(List<InvCountHeaderDTO> invCountHeaderDTO);

    InvCountInfoDTO checkAndRemove(List<InvCountHeaderDTO> invCountHeaders);

    InvCountHeaderDTO detail(Long countHeaderId);

    List<InvCountHeaderDTO> executeCheck(List<InvCountHeaderDTO> invCountHeaders);

    List<InvCountHeaderDTO> execute(List<InvCountHeaderDTO> invCountHeaders);

    InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaderList);

    InvCountHeaderDTO countResultSync(InvCountHeaderDTO invCountHeader);

    InvCountInfoDTO submitCheck(List<InvCountHeaderDTO> invCountHeaders);

    List<InvCountHeaderDTO> submit(List<InvCountHeaderDTO> invCountHeaders);

    void callBack(Long organizationId, WorkFlowDTO workFlowDTO);

    List<InvCountHeaderDTO> countingOrderReportDs(InvCountHeaderDTO invCountHeaderDTO);
}

