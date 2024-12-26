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
import com.hand.demo.app.service.IamCompanyService;
import com.hand.demo.domain.entity.IamCompany;
import com.hand.demo.domain.repository.IamCompanyRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (IamCompany)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:27:26
 */

@RestController("iamCompanyController.v1")
@RequestMapping("/v1/{organizationId}/iam-companys")
public class IamCompanyController extends BaseController {

    @Autowired
    private IamCompanyRepository iamCompanyRepository;

    @Autowired
    private IamCompanyService iamCompanyService;

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    public ResponseEntity<Page<IamCompany>> list(IamCompany iamCompany, @PathVariable Long organizationId,
                                                 @ApiIgnore @SortDefault(value = IamCompany.FIELD_COMPANY_ID,
                                                         direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<IamCompany> list = iamCompanyService.selectList(pageRequest, iamCompany);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{companyId}/detail")
    public ResponseEntity<IamCompany> detail(@PathVariable Long companyId) {
        IamCompany iamCompany = iamCompanyRepository.selectByPrimary(companyId);
        return Results.success(iamCompany);
    }

    @ApiOperation(value = "创建或更新")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping
    public ResponseEntity<List<IamCompany>> save(@PathVariable Long organizationId, @RequestBody List<IamCompany> iamCompanys) {
        validObject(iamCompanys);
        SecurityTokenHelper.validTokenIgnoreInsert(iamCompanys);
        iamCompanys.forEach(item -> item.setTenantId(organizationId));
        iamCompanyService.saveData(iamCompanys);
        return Results.success(iamCompanys);
    }

    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody List<IamCompany> iamCompanys) {
        SecurityTokenHelper.validToken(iamCompanys);
        iamCompanyRepository.batchDeleteByPrimaryKey(iamCompanys);
        return Results.success();
    }

}

