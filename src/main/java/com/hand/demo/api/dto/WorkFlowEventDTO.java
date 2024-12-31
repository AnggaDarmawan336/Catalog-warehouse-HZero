package com.hand.demo.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class WorkFlowEventDTO {
    private String businessKey;
    private String docStatus;
    private Long workflowId;
    private Date approvedTime;
}
