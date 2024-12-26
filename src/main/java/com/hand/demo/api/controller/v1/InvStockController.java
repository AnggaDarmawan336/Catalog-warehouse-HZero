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
import com.hand.demo.app.service.InvStockService;
import com.hand.demo.domain.entity.InvStock;
import com.hand.demo.domain.repository.InvStockRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvStock)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:29:00
 */

@RestController("invStockController.v1")
@RequestMapping("/v1/{organizationId}/inv-stocks")
public class InvStockController extends BaseController {

    @Autowired
    private InvStockRepository invStockRepository;

    @Autowired
    private InvStockService invStockService;

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    public ResponseEntity<Page<InvStock>> list(InvStock invStock, @PathVariable Long organizationId,
                                               @ApiIgnore @SortDefault(value = InvStock.FIELD_STOCK_ID,
                                                       direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<InvStock> list = invStockService.selectList(pageRequest, invStock);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{stockId}/detail")
    public ResponseEntity<InvStock> detail(@PathVariable Long stockId) {
        InvStock invStock = invStockRepository.selectByPrimary(stockId);
        return Results.success(invStock);
    }

    @ApiOperation(value = "创建或更新")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping
    public ResponseEntity<List<InvStock>> save(@PathVariable Long organizationId, @RequestBody List<InvStock> invStocks) {
        validObject(invStocks);
        SecurityTokenHelper.validTokenIgnoreInsert(invStocks);
        invStocks.forEach(item -> item.setTenantId(organizationId));
        invStockService.saveData(invStocks);
        return Results.success(invStocks);
    }

    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody List<InvStock> invStocks) {
        SecurityTokenHelper.validToken(invStocks);
        invStockRepository.batchDeleteByPrimaryKey(invStocks);
        return Results.success();
    }

}

