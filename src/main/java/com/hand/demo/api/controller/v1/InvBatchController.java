package com.hand.demo.api.controller.v1;

import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import org.hzero.core.base.BaseController;
import org.hzero.core.util.Results;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hand.demo.app.service.InvBatchService;
import com.hand.demo.domain.entity.InvBatch;
import com.hand.demo.domain.repository.InvBatchRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvBatch)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:27:55
 */

@RestController("invBatchController.v1")
@RequestMapping("/v1/{organizationId}/inv-batchs")
public class InvBatchController extends BaseController {

    @Autowired
    private InvBatchRepository invBatchRepository;

    @Autowired
    private InvBatchService invBatchService;

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    public ResponseEntity<Page<InvBatch>> list(InvBatch invBatch, @PathVariable Long organizationId,
                                               @ApiIgnore @SortDefault(value = InvBatch.FIELD_BATCH_ID,
                                                       direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<InvBatch> list = invBatchService.selectList(pageRequest, invBatch);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{batchId}/detail")
    public ResponseEntity<InvBatch> detail(@PathVariable Long batchId) {
        InvBatch invBatch = invBatchRepository.selectByPrimary(batchId);
        return Results.success(invBatch);
    }

    @ApiOperation(value = "创建或更新")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping
    public ResponseEntity<List<InvBatch>> save(@PathVariable Long organizationId, @RequestBody List<InvBatch> invBatchs) {
        validObject(invBatchs);
        SecurityTokenHelper.validTokenIgnoreInsert(invBatchs);
        invBatchs.forEach(item -> item.setTenantId(organizationId));
        invBatchService.saveData(invBatchs);
        return Results.success(invBatchs);
    }

    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody List<InvBatch> invBatchs) {
        SecurityTokenHelper.validToken(invBatchs);
        invBatchRepository.batchDeleteByPrimaryKey(invBatchs);
        return Results.success();
    }

}

