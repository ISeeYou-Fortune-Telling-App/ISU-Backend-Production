package com.iseeyou.fortunetelling.config.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iseeyou.fortunetelling.dto.request.servicepackage.AvailableTimeSlotRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Converter để chuyển đổi JSON string thành List<AvailableTimeSlotRequest>
 * Được sử dụng khi gửi dữ liệu dạng multipart/form-data
 * Hỗ trợ cả single object và array
 */
@Component
@Slf4j
public class StringToAvailableTimeSlotsConverter implements Converter<String, List<AvailableTimeSlotRequest>> {

    private final ObjectMapper objectMapper;

    public StringToAvailableTimeSlotsConverter() {
        this.objectMapper = new ObjectMapper();
        // Đăng ký module để hỗ trợ LocalTime, LocalDate, LocalDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public List<AvailableTimeSlotRequest> convert(@NonNull String source) {
        try {
            if (source == null || source.trim().isEmpty() || source.equalsIgnoreCase("null")) {
                log.debug("Empty or null availableTimeSlots, returning null");
                return null;
            }

            String jsonSource = source.trim();

            // Kiểm tra nếu là một mảng JSON
            if (jsonSource.startsWith("[")) {
                log.debug("Parsing as JSON array: {}", jsonSource);
                List<AvailableTimeSlotRequest> result = objectMapper.readValue(
                    jsonSource,
                    new TypeReference<List<AvailableTimeSlotRequest>>() {}
                );
                log.debug("Successfully converted to {} time slots", result != null ? result.size() : 0);
                return result;
            }

            // Kiểm tra nếu là một object JSON đơn
            if (jsonSource.startsWith("{")) {
                log.debug("Parsing as single JSON object, converting to array: {}", jsonSource);
                AvailableTimeSlotRequest singleSlot = objectMapper.readValue(
                    jsonSource,
                    AvailableTimeSlotRequest.class
                );
                List<AvailableTimeSlotRequest> result = new ArrayList<>();
                result.add(singleSlot);
                log.debug("Successfully converted single object to list with 1 time slot");
                return result;
            }

            // Nếu không bắt đầu bằng [ hoặc {, thử thêm [] để tạo thành array
            jsonSource = "[" + jsonSource + "]";
            log.debug("Added array brackets to source: {}", jsonSource);

            List<AvailableTimeSlotRequest> result = objectMapper.readValue(
                jsonSource,
                new TypeReference<List<AvailableTimeSlotRequest>>() {}
            );

            log.debug("Successfully converted to {} time slots", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("Failed to convert string to List<AvailableTimeSlotRequest>: {}", source, e);
            throw new IllegalArgumentException("Định dạng availableTimeSlots không hợp lệ. " +
                "Vui lòng gửi mảng JSON hoặc object JSON với format: " +
                "[{\"weekDate\":2,\"availableFrom\":\"09:00:00\",\"availableTo\":\"12:00:00\"}] " +
                "hoặc {\"weekDate\":2,\"availableFrom\":\"09:00:00\",\"availableTo\":\"12:00:00\"}. " +
                "Lỗi: " + e.getMessage());
        }
    }
}
