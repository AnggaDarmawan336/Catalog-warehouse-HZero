package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hzero.boot.workflow.dto.RunTaskHistory;
import org.hzero.common.HZeroCacheKey;
import org.hzero.core.cache.CacheValue;
import org.hzero.core.cache.Cacheable;

import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class InvCountHeaderDTO extends InvCountHeader implements Cacheable {
    @ApiModelProperty(value = "Error Message")
    private String errorMsg;

    private List<InvCountLineDTO> countOrderLineList;

    private List<InvWarehouseDTO> invWarehouseDTOList;

    private List<UserDTO> counterIdList;

    private List<UserDTO> supervisorIdList;

    private List<MaterialDTO> snapshotMaterialList;

    private List<BatchDTO> snapshotBatchList;

    private List<InvStock> stockList;

    private Boolean tenantAdminFlag;

    private String employeeNumber;

    private String departmentName;

    private String departmentCode;

    private String materialCode;

    private String batchCode;

    private String status;

    private Boolean isWMSWarehouse;

    private List<RunTaskHistory> history;

}
