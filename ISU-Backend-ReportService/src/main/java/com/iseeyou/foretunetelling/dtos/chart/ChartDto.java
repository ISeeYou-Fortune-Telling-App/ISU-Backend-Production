package com.iseeyou.foretunetelling.dtos.chart;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ChartDto<T> {
    private Integer month;
    private Integer year;
    private Map<T, BigDecimal> data;
}
