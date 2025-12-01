package com.iseeyou.foretunetelling.mapper;

import com.iseeyou.foretunetelling.dtos.CustomerPotentialDto;
import com.iseeyou.foretunetelling.dtos.SeerPerformanceDto;
import com.iseeyou.foretunetelling.models.CustomerPotential;
import com.iseeyou.foretunetelling.models.SeerPerformance;
import com.iseeyou.foretunetelling.utils.Constants;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SimpleMapper extends BaseMapper {

    @Autowired
    public SimpleMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected void configureCustomMappings() {
        // Converter for LocalDateTime to Date
        Converter<LocalDateTime, Date> localDateTimeToDate = ctx ->
            ctx.getSource() == null ? null : Date.from(ctx.getSource().atZone(ZoneId.systemDefault()).toInstant());

        // Converter for BigDecimal to Double
        Converter<BigDecimal, Double> bigDecimalToDouble = ctx ->
            ctx.getSource() == null ? null : ctx.getSource().doubleValue();

        // Converter for CustomerTier enum to String
        Converter<Constants.CustomerTier, String> customerTierToString = ctx ->
            ctx.getSource() == null ? null : ctx.getSource().name();

        // Converter for SeerTier enum to String
        Converter<Constants.SeerTier, String> seerTierToString = ctx ->
            ctx.getSource() == null ? null : ctx.getSource().name();

        // Mapping CustomerPotential to CustomerPotentialDto
        TypeMap<CustomerPotential, CustomerPotentialDto> customerPotentialTypeMap =
            modelMapper.createTypeMap(CustomerPotential.class, CustomerPotentialDto.class);
        customerPotentialTypeMap.addMappings(mapper -> {
            mapper.using(customerTierToString).map(CustomerPotential::getPotentialTier, CustomerPotentialDto::setPotentialTier);
            mapper.using(bigDecimalToDouble).map(CustomerPotential::getTotalSpending, CustomerPotentialDto::setTotalSpending);
            mapper.using(localDateTimeToDate).map(CustomerPotential::getCreatedAt, CustomerPotentialDto::setCreatedAt);
            mapper.using(localDateTimeToDate).map(CustomerPotential::getUpdatedAt, CustomerPotentialDto::setUpdatedAt);
        });

        // Mapping SeerPerformance to SeerPerformanceDto
        TypeMap<SeerPerformance, SeerPerformanceDto> seerPerformanceTypeMap =
            modelMapper.createTypeMap(SeerPerformance.class, SeerPerformanceDto.class);
        seerPerformanceTypeMap.addMappings(mapper -> {
            mapper.using(seerTierToString).map(SeerPerformance::getPerformanceTier, SeerPerformanceDto::setPerformanceTier);
            mapper.using(bigDecimalToDouble).map(SeerPerformance::getTotalRevenue, SeerPerformanceDto::setTotalRevenue);
            mapper.using(bigDecimalToDouble).map(SeerPerformance::getBonus, SeerPerformanceDto::setBonus);
            mapper.using(localDateTimeToDate).map(SeerPerformance::getCreatedAt, SeerPerformanceDto::setCreatedAt);
            mapper.using(localDateTimeToDate).map(SeerPerformance::getUpdatedAt, SeerPerformanceDto::setUpdatedAt);
        });
    }

    @Override
    public <T> T mapTo(Object source, Class<T> targetType) {
        if (source == null) return null;
        return super.mapTo(source, targetType);
    }

    @Override
    public <T> List<T> mapToList(List<?> sourceList, Class<T> targetType) {
        if (sourceList == null || sourceList.isEmpty()) return List.of();
        return sourceList.stream()
                .map(source -> mapTo(source, targetType))
                .collect(Collectors.toList());
    }

    @Override
    public <T> Page<T> mapToPage(Page<?> sourcePage, Class<T> targetType) {
        if (sourcePage == null) return Page.empty();
        return sourcePage.map(source -> mapTo(source, targetType));
    }
}
