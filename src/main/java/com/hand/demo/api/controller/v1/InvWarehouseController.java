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
import com.hand.demo.app.service.InvWarehouseService;
import com.hand.demo.domain.entity.InvWarehouse;
import com.hand.demo.domain.repository.InvWarehouseRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvWarehouse)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:29:12
 */

@RestController("invWarehouseController.v1")
@RequestMapping("/v1/{organizationId}/inv-warehouses")
public class InvWarehouseController extends BaseController {

    @Autowired
    private InvWarehouseRepository invWarehouseRepository;

    @Autowired
    private InvWarehouseService invWarehouseService;

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    public ResponseEntity<Page<InvWarehouse>> list(InvWarehouse invWarehouse, @PathVariable Long organizationId,
                                                   @ApiIgnore @SortDefault(value = InvWarehouse.FIELD_WAREHOUSE_ID,
                                                           direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<InvWarehouse> list = invWarehouseService.selectList(pageRequest, invWarehouse);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{warehouseId}/detail")
    public ResponseEntity<InvWarehouse> detail(@PathVariable Long warehouseId) {
        InvWarehouse invWarehouse = invWarehouseRepository.selectByPrimary(warehouseId);
        return Results.success(invWarehouse);
    }

    @ApiOperation(value = "创建或更新")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping
    public ResponseEntity<List<InvWarehouse>> save(@PathVariable Long organizationId, @RequestBody List<InvWarehouse> invWarehouses) {
        validObject(invWarehouses);
        SecurityTokenHelper.validTokenIgnoreInsert(invWarehouses);
        invWarehouses.forEach(item -> item.setTenantId(organizationId));
        invWarehouseService.saveData(invWarehouses);
        return Results.success(invWarehouses);
    }

    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody List<InvWarehouse> invWarehouses) {
        SecurityTokenHelper.validToken(invWarehouses);
        invWarehouseRepository.batchDeleteByPrimaryKey(invWarehouses);
        return Results.success();
    }

}

