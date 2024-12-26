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
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * (InvStock)实体类
 *
 * @author Angga
 * @since 2024-12-17 14:29:00
 */

@Getter
@Setter
@ApiModel("")
@VersionAudit
@ModifyAudit
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Table(name = "fexam_inv_stock")
public class InvStock extends AuditDomain {
    private static final long serialVersionUID = -66333274519836538L;

    public static final String FIELD_STOCK_ID = "stockId";
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
    public static final String FIELD_AVAILABLE_QUANTITY = "availableQuantity";
    public static final String FIELD_BATCH_ID = "batchId";
    public static final String FIELD_COMPANY_ID = "companyId";
    public static final String FIELD_DEPARTMENT_ID = "departmentId";
    public static final String FIELD_MATERIAL_CODE = "materialCode";
    public static final String FIELD_MATERIAL_ID = "materialId";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_UNIT_CODE = "unitCode";
    public static final String FIELD_UNIT_QUANTITY = "unitQuantity";
    public static final String FIELD_WAREHOUSE_ID = "warehouseId";

    @Id
    @GeneratedValue
    private Long stockId;

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

    @ApiModelProperty(value = "", required = true)
    @NotNull
    private BigDecimal availableQuantity;

    private Long batchId;

    private Long companyId;

    private Long departmentId;

    @ApiModelProperty(value = "", required = true)
    @NotBlank
    private String materialCode;

    private Long materialId;

    @ApiModelProperty(value = "", required = true)
    @NotNull
    private Long tenantId;

    @ApiModelProperty(value = "", required = true)
    @NotBlank
    private String unitCode;

    @ApiModelProperty(value = "", required = true)
    @NotNull
    private BigDecimal unitQuantity;

    private Long warehouseId;

    @Transient
    private String materialIds;

    @Transient
    private String batchIds;
}

