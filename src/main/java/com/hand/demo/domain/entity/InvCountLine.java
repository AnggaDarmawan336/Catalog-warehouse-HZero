package com.hand.demo.domain.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.VersionAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * (InvCountLine)实体类
 *
 * @author Angga
 * @since 2024-12-17 14:28:32
 */

@Getter
@Setter
@ApiModel("")
@VersionAudit
@ModifyAudit
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Table(name = "fexam_inv_count_line")
@Accessors(chain = true)
public class InvCountLine extends AuditDomain {
    private static final long serialVersionUID = 734950587556433756L;

    public static final String FIELD_COUNT_LINE_ID = "countLineId";
    public static final String FIELD_ATTRIBUTE1 = "attribute1";
    public static final String FIELD_ATTRIBUTE10 = "attribute10";
    public static final String FIELD_ATTRIBUTE11 = "attribute11";
    public static final String FIELD_ATTRIBUTE12 = "attribute12";
    public static final String FIELD_ATTRIBUTE13 = "attribute13";
    public static final String FIELD_ATTRIBUTE14 = "attribute14";
    public static final String FIELD_ATTRIBUTE15 = "attribute15";
    public static final String FIELD_ATTRIBUTE2 = "attribute2";
    public static final String FIELD_ATTRIBUTE3 = "attribute3";
    public static final String FIELD_ATTRIBUTE4 = "attribute4";
    public static final String FIELD_ATTRIBUTE5 = "attribute5";
    public static final String FIELD_ATTRIBUTE6 = "attribute6";
    public static final String FIELD_ATTRIBUTE7 = "attribute7";
    public static final String FIELD_ATTRIBUTE8 = "attribute8";
    public static final String FIELD_ATTRIBUTE9 = "attribute9";
    public static final String FIELD_ATTRIBUTE_CATEGORY = "attributeCategory";
    public static final String FIELD_BATCH_ID = "batchId";
    public static final String FIELD_COUNT_HEADER_ID = "countHeaderId";
    public static final String FIELD_COUNTER_IDS = "counterIds";
    public static final String FIELD_LINE_NUMBER = "lineNumber";
    public static final String FIELD_MATERIAL_ID = "materialId";
    public static final String FIELD_REMARK = "remark";
    public static final String FIELD_SNAPSHOT_UNIT_QTY = "snapshotUnitQty";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_UNIT_CODE = "unitCode";
    public static final String FIELD_UNIT_DIFF_QTY = "unitDiffQty";
    public static final String FIELD_UNIT_QTY = "unitQty";
    public static final String FIELD_WAREHOUSE_ID = "warehouseId";

    @Id
    @GeneratedValue
    private Long countLineId;

    private String attribute1;

    private String attribute10;

    private String attribute11;

    private String attribute12;

    private String attribute13;

    private String attribute14;

    private String attribute15;

    private String attribute2;

    private String attribute3;

    private String attribute4;

    private String attribute5;

    private String attribute6;

    private String attribute7;

    private String attribute8;

    private String attribute9;

    private String attributeCategory;

    private Long batchId;

    @ApiModelProperty(value = "", required = true)
    @NotNull
    private Long countHeaderId;

    private String counterIds;

    @ApiModelProperty(value = "", required = true)
    @NotNull
    private Integer lineNumber;

    private Long materialId;

    private String remark;

    private BigDecimal snapshotUnitQty;

    @ApiModelProperty(value = "", required = true)
    @NotNull
    private Long tenantId = 0L;

    private String unitCode;

    @ApiModelProperty(value = "unit_diff_qty = unit_qty - snapshot_unit_qty")
    private BigDecimal unitDiffQty;

    private BigDecimal unitQty;

    private Long warehouseId;


}

