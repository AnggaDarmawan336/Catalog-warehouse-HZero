package com.hand.demo.api.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InvCountInfoDTO {
    private String totalErrorMsg;

    @ApiModelProperty(value = "Verification Passed List")
    private List<InvCountHeaderDTO> successList;

    @ApiModelProperty(value = "Verification Failed List")
    private List<InvCountHeaderDTO> failedList;
}
