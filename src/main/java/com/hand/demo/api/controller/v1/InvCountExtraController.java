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
import com.hand.demo.app.service.InvCountExtraService;
import com.hand.demo.domain.entity.InvCountExtra;
import com.hand.demo.domain.repository.InvCountExtraRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvCountExtra)表控制层
 *
 * @author Angga
 * @since 2024-12-17 14:28:08
 */

@RestController("invCountExtraController.v1")
@RequestMapping("/v1/{organizationId}/inv-count-extras")
public class InvCountExtraController extends BaseController {

    @Autowired
    private InvCountExtraRepository invCountExtraRepository;

    @Autowired
    private InvCountExtraService invCountExtraService;

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping
    public ResponseEntity<Page<InvCountExtra>> list(InvCountExtra invCountExtra, @PathVariable Long organizationId,
                                                    @ApiIgnore @SortDefault(value = InvCountExtra.FIELD_EXTRA_INFO_ID,
                                                            direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<InvCountExtra> list = invCountExtraService.selectList(pageRequest, invCountExtra);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @GetMapping("/{extraInfoId}/detail")
    public ResponseEntity<InvCountExtra> detail(@PathVariable Long extraInfoId) {
        InvCountExtra invCountExtra = invCountExtraRepository.selectByPrimary(extraInfoId);
        return Results.success(invCountExtra);
    }

    @ApiOperation(value = "创建或更新")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @PostMapping
    public ResponseEntity<List<InvCountExtra>> save(@PathVariable Long organizationId, @RequestBody List<InvCountExtra> invCountExtras) {
        validObject(invCountExtras);
        SecurityTokenHelper.validTokenIgnoreInsert(invCountExtras);
        invCountExtras.forEach(item -> item.setTenantId(organizationId));
        invCountExtraService.saveData(invCountExtras);
        return Results.success(invCountExtras);
    }

    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody List<InvCountExtra> invCountExtras) {
        SecurityTokenHelper.validToken(invCountExtras);
        invCountExtraRepository.batchDeleteByPrimaryKey(invCountExtras);
        return Results.success();
    }

}

