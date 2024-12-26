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
import com.hand.demo.app.service.IamDepartmentService;
import com.hand.demo.domain.entity.IamDepartment;
import com.hand.demo.domain.repository.IamDepartmentRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (IamDepartment)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:27:42
 */

@RestController("iamDepartmentController.v1")
@RequestMapping("/v1/{organizationId}/iam-departments")
public class IamDepartmentController extends BaseController {

    @Autowired
    private IamDepartmentRepository iamDepartmentRepository;

    @Autowired
    private IamDepartmentService iamDepartmentService;

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    public ResponseEntity<Page<IamDepartment>> list(IamDepartment iamDepartment, @PathVariable Long organizationId,
                                                    @ApiIgnore @SortDefault(value = IamDepartment.FIELD_DEPARTMENT_ID,
                                                            direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<IamDepartment> list = iamDepartmentService.selectList(pageRequest, iamDepartment);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{departmentId}/detail")
    public ResponseEntity<IamDepartment> detail(@PathVariable Long departmentId) {
        IamDepartment iamDepartment = iamDepartmentRepository.selectByPrimary(departmentId);
        return Results.success(iamDepartment);
    }

    @ApiOperation(value = "创建或更新")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping
    public ResponseEntity<List<IamDepartment>> save(@PathVariable Long organizationId, @RequestBody List<IamDepartment> iamDepartments) {
        validObject(iamDepartments);
        SecurityTokenHelper.validTokenIgnoreInsert(iamDepartments);
        iamDepartments.forEach(item -> item.setTenantId(organizationId));
        iamDepartmentService.saveData(iamDepartments);
        return Results.success(iamDepartments);
    }

    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody List<IamDepartment> iamDepartments) {
        SecurityTokenHelper.validToken(iamDepartments);
        iamDepartmentRepository.batchDeleteByPrimaryKey(iamDepartments);
        return Results.success();
    }

}

