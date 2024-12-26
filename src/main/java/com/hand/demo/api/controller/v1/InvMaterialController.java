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
import com.hand.demo.app.service.InvMaterialService;
import com.hand.demo.domain.entity.InvMaterial;
import com.hand.demo.domain.repository.InvMaterialRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvMaterial)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:28:46
 */

@RestController("invMaterialController.v1")
@RequestMapping("/v1/{organizationId}/inv-materials")
public class InvMaterialController extends BaseController {

    @Autowired
    private InvMaterialRepository invMaterialRepository;

    @Autowired
    private InvMaterialService invMaterialService;

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    public ResponseEntity<Page<InvMaterial>> list(InvMaterial invMaterial, @PathVariable Long organizationId,
                                                  @ApiIgnore @SortDefault(value = InvMaterial.FIELD_MATERIAL_ID,
                                                          direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<InvMaterial> list = invMaterialService.selectList(pageRequest, invMaterial);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{materialId}/detail")
    public ResponseEntity<InvMaterial> detail(@PathVariable Long materialId) {
        InvMaterial invMaterial = invMaterialRepository.selectByPrimary(materialId);
        return Results.success(invMaterial);
    }

    @ApiOperation(value = "创建或更新")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping
    public ResponseEntity<List<InvMaterial>> save(@PathVariable Long organizationId, @RequestBody List<InvMaterial> invMaterials) {
        validObject(invMaterials);
        SecurityTokenHelper.validTokenIgnoreInsert(invMaterials);
        invMaterials.forEach(item -> item.setTenantId(organizationId));
        invMaterialService.saveData(invMaterials);
        return Results.success(invMaterials);
    }

    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody List<InvMaterial> invMaterials) {
        SecurityTokenHelper.validToken(invMaterials);
        invMaterialRepository.batchDeleteByPrimaryKey(invMaterials);
        return Results.success();
    }

}

