package com.iseeyou.foretunetelling.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class FinanceStatistic extends AbstractBaseResponse {
    private BigDecimal totalRevenue;
    private Double percentChangeTotalRevenue;
    private BigDecimal totalNet;
    private Double percentChangeTotalNet;
    private BigDecimal totalTax;
    private Double percentChangeTotalTax;
    private BigDecimal totalRevenueDay;
    private Double percentChangeTotalRevenueDay;
}
