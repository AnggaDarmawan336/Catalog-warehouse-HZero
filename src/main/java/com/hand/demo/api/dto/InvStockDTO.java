package com.hand.demo.api.dto;

import com.hand.demo.domain.entity.InvStock;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InvStockDTO extends InvStock {
    private Boolean isLot;
    private BigDecimal summary;

    private List<Long> materialIdLongList;
    private List<Long> batchIdLongList;
}
