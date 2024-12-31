package com.hand.demo.api.controller.v1;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.WorkFlowEventDTO;
import com.hand.demo.infra.mapper.InvCountHeaderMapper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.base.BaseController;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.core.util.Results;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hand.demo.app.service.InvCountHeaderService;
import com.hand.demo.domain.entity.InvCountHeader;
import com.hand.demo.domain.repository.InvCountHeaderRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvCountHeader)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:28:19
 */

@RestController("invCountHeaderController.v1")
@RequestMapping("/v1/{organizationId}/inv-count-headers")
public class InvCountHeaderController extends BaseController {

    @Autowired
    private InvCountHeaderRepository invCountHeaderRepository;

    @Autowired
    private InvCountHeaderService invCountHeaderService;

    @Autowired
    InvCountHeaderMapper invCountHeaderMapper;

    @ApiOperation(value = "List")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    public ResponseEntity<Page<InvCountHeaderDTO>> list(InvCountHeaderDTO invCountHeader, @PathVariable Long organizationId,
                                                     @ApiIgnore @SortDefault(value = InvCountHeader.FIELD_CREATION_DATE,
                                                             direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<InvCountHeaderDTO> list = invCountHeaderService.selectList(pageRequest, invCountHeader);
        return Results.success(list);
    }

    @ApiOperation(value = "Detail")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/detail")
    @ProcessCacheValue
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    public ResponseEntity <InvCountHeaderDTO> detail(@RequestBody @SortDefault(value = InvCountHeaderDTO.FIELD_CREATION_DATE,
            direction = Sort.Direction.DESC) Long countHeaderId) {
        InvCountHeaderDTO invCountHeader = invCountHeaderService.detail(countHeaderId);
        return Results.success(invCountHeader);
    }

    @ApiOperation(value = "Manual Save")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/manual-save")
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    public ResponseEntity<List<InvCountHeaderDTO>> orderSave(@PathVariable Long organizationId, @RequestBody List<InvCountHeaderDTO> invCountHeader) {
        validList(invCountHeader, InvCountHeader.validateCreate.class);
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeader);

        invCountHeader.forEach(item -> item.setTenantId(organizationId));
        invCountHeaderService.manualSave(invCountHeader);
        return Results.success(invCountHeader);
    }

    @ApiOperation(value = "Check and Remove")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @DeleteMapping
    public ResponseEntity<?> orderRemove(@RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        invCountHeaderService.checkAndRemove(invCountHeaders);
        return Results.success("Delete Successfully!");
    }

    @ApiOperation(value = "Execute")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/execute")
    public ResponseEntity<?> orderExecution(@RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.validateExecute.class, InvCountHeader.validateCreate.class);
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        invCountHeaderService.orderExecution(invCountHeaders);
        return Results.success(invCountHeaders);
    }

    @ApiOperation(value = "Result Synchronous")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/result-synchronous")
    public ResponseEntity<?> countResultSync(@RequestBody InvCountHeaderDTO invCountHeaders) {
        invCountHeaderService.countResultSync(invCountHeaders);
        return Results.success(invCountHeaders);
    }

    @ApiOperation(value = "Order Submit")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/order-submit")
    public ResponseEntity<?> orderSubmit(@RequestBody List<InvCountHeaderDTO> invCountHeaders) {
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        invCountHeaderService.orderSubmit(invCountHeaders);
        return Results.success(invCountHeaders);
    }

    @ApiOperation(value = "callBack")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping("/call-back")
    public ResponseEntity<?> callBack(@RequestBody WorkFlowEventDTO invCountHeaders) {
        invCountHeaderService.callBack(invCountHeaders);
        return Results.success(invCountHeaders);
    }

    @ApiOperation(value = "Counting Order Report Ds")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/counting-order-report-ds")
    @ProcessCacheValue
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    public ResponseEntity<?> countingOrderReportDs(InvCountHeaderDTO invCountHeader) {
        return Results.success(invCountHeaderService.countingOrderReportDs(invCountHeader));
    }
}

