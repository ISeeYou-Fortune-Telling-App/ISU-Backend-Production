package com.iseeyou.foretunetelling.controllers;

import com.iseeyou.foretunetelling.controllers.base.AbstractBaseController;
import com.iseeyou.foretunetelling.dtos.CustomerPotentialDto;
import com.iseeyou.foretunetelling.dtos.PageResponse;
import com.iseeyou.foretunetelling.dtos.SeerPerformanceDto;
import com.iseeyou.foretunetelling.dtos.SingleResponse;
import com.iseeyou.foretunetelling.mapper.SimpleMapper;
import com.iseeyou.foretunetelling.services.ReportService;
import com.iseeyou.foretunetelling.utils.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("")
@Tag(name = "001. Statistic Report", description = "Statistic Report API")
@Slf4j
public class ReportStatisticController extends AbstractBaseController {
    private final ReportService reportService;
    private final SimpleMapper simpleMapper;

    @GetMapping("/customer-potential")
    @Operation(
            summary = "Get customer potential report by customerId, month and year",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<CustomerPotentialDto>> getCustomerPotential(
            @Parameter(description = "Customer ID")
            @RequestParam String customerId,
            @Parameter(description = "Month (1-12)")
            @RequestParam Integer month,
            @Parameter(description = "Year")
            @RequestParam Integer year
    ) {
        CustomerPotentialDto customerPotential = simpleMapper.mapTo(
                reportService.getCustomerPotential(customerId, month, year),
                CustomerPotentialDto.class
        );
        return responseFactory.successSingle(customerPotential, "Customer potential retrieved successfully");
    }

    @GetMapping("/my-customer-potential")
    @Operation(
            summary = "Get my customer potential report by month and year",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<CustomerPotentialDto>> getMyCustomerPotential(
            @Parameter(description = "Month (1-12)")
            @RequestParam Integer month,
            @Parameter(description = "Year")
            @RequestParam Integer year
    ) {
        CustomerPotentialDto customerPotential = simpleMapper.mapTo(
                reportService.getMyCustomerPotential(month, year),
                CustomerPotentialDto.class
        );
        return responseFactory.successSingle(customerPotential, "My customer potential retrieved successfully");
    }

    @GetMapping("/seer-performance")
    @Operation(
            summary = "Get seer performance report by seerId, month and year",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<SeerPerformanceDto>> getSeerPerformance(
            @Parameter(description = "Seer ID")
            @RequestParam String seerId,
            @Parameter(description = "Month (1-12)")
            @RequestParam Integer month,
            @Parameter(description = "Year")
            @RequestParam Integer year
    ) {
        SeerPerformanceDto seerPerformance = simpleMapper.mapTo(
                reportService.getSeerPerformance(seerId, month, year),
                SeerPerformanceDto.class
        );
        return responseFactory.successSingle(seerPerformance, "Seer performance retrieved successfully");
    }

    @GetMapping("/my-seer-performance")
    @Operation(
            summary = "Get my seer performance report by month and year",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<SeerPerformanceDto>> getMySeerPerformance(
            @Parameter(description = "Month (1-12)")
            @RequestParam Integer month,
            @Parameter(description = "Year")
            @RequestParam Integer year
    ) {
        SeerPerformanceDto seerPerformance = simpleMapper.mapTo(
                reportService.getMySeerPerformance(month, year),
                SeerPerformanceDto.class
        );
        return responseFactory.successSingle(seerPerformance, "My seer performance retrieved successfully");
    }

    @PostMapping("/customer-reports")
    @Operation(
            summary = "Create customer reports for multiple customers",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<Boolean>> createListCustomerReport(
            @Parameter(description = "List of customer IDs")
            @RequestBody List<String> customerIds,
            @Parameter(description = "Month (1-12)")
            @RequestParam Integer month,
            @Parameter(description = "Year")
            @RequestParam Integer year
    ) {
        boolean result = reportService.createListCustomerReport(customerIds, month, year);
        return responseFactory.successSingle(result, "Customer reports created successfully");
    }

    @PostMapping("/seer-performance-reports")
    @Operation(
            summary = "Create seer performance reports for multiple seers",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<SingleResponse<Boolean>> createListSeerPerformanceReport(
            @Parameter(description = "List of seer IDs")
            @RequestBody List<String> seerIds,
            @Parameter(description = "Month (1-12)")
            @RequestParam Integer month,
            @Parameter(description = "Year")
            @RequestParam Integer year
    ) {
        boolean result = reportService.createListSeerPerformanceReport(seerIds, month, year);
        return responseFactory.successSingle(result, "Seer performance reports created successfully");
    }

    @GetMapping("/all-customer-potential")
    @Operation(
            summary = "Get all customer potential reports with pagination and optional month/year filter",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<CustomerPotentialDto>> getAllCustomerPotential(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "15") int limit,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Month (1-12)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Year")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Minimum potential point")
            @RequestParam(required = false) Integer minPotentialPoint,
            @Parameter(description = "Maximum potential point")
            @RequestParam(required = false) Integer maxPotentialPoint,
            @Parameter(description = "Potential tier (CASUAL, STANDARD, PREMIUM, VIP)")
            @RequestParam(required = false) Constants.CustomerTier potentialTier,
            @Parameter(description = "Minimum ranking")
            @RequestParam(required = false) Integer minRanking,
            @Parameter(description = "Maximum ranking")
            @RequestParam(required = false) Integer maxRanking,
            @Parameter(description = "Minimum total booking requests")
            @RequestParam(required = false) Integer minTotalBookingRequests,
            @Parameter(description = "Maximum total booking requests")
            @RequestParam(required = false) Integer maxTotalBookingRequests,
            @Parameter(description = "Minimum total spending")
            @RequestParam(required = false) BigDecimal minTotalSpending,
            @Parameter(description = "Maximum total spending")
            @RequestParam(required = false) BigDecimal maxTotalSpending,
            @Parameter(description = "Minimum cancelled by customer")
            @RequestParam(required = false) Integer minCancelledByCustomer,
            @Parameter(description = "Maximum cancelled by customer")
            @RequestParam(required = false) Integer maxCancelledByCustomer
    ) {
        log.info("Get all customer potential - page: {}, limit: {}, month: {}, year: {}", page, limit, month, year);
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        return responseFactory.successPage(
                simpleMapper.mapToPage(
                        reportService.getAllCustomerPotential(
                                pageable, month, year,
                                minPotentialPoint, maxPotentialPoint,
                                potentialTier,
                                minRanking, maxRanking,
                                minTotalBookingRequests, maxTotalBookingRequests,
                                minTotalSpending, maxTotalSpending,
                                minCancelledByCustomer, maxCancelledByCustomer
                        ),
                        CustomerPotentialDto.class
                ),
                "Customer potential reports retrieved successfully"
        );
    }

    @GetMapping("/all-seer-performance")
    @Operation(
            summary = "Get all seer performance reports with pagination and optional month/year filter",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<SeerPerformanceDto>> getAllSeerPerformance(
            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "15") int limit,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortType,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Month (1-12)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Year")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Minimum performance point")
            @RequestParam(required = false) Integer minPerformancePoint,
            @Parameter(description = "Maximum performance point")
            @RequestParam(required = false) Integer maxPerformancePoint,
            @Parameter(description = "Performance tier (APPRENTICE, PROFESSIONAL, EXPERT, MASTER)")
            @RequestParam(required = false) Constants.SeerTier performanceTier,
            @Parameter(description = "Minimum ranking")
            @RequestParam(required = false) Integer minRanking,
            @Parameter(description = "Maximum ranking")
            @RequestParam(required = false) Integer maxRanking,
            @Parameter(description = "Minimum total packages")
            @RequestParam(required = false) Integer minTotalPackages,
            @Parameter(description = "Maximum total packages")
            @RequestParam(required = false) Integer maxTotalPackages,
            @Parameter(description = "Minimum total rates")
            @RequestParam(required = false) Integer minTotalRates,
            @Parameter(description = "Maximum total rates")
            @RequestParam(required = false) Integer maxTotalRates,
            @Parameter(description = "Minimum average rating")
            @RequestParam(required = false) Double minAvgRating,
            @Parameter(description = "Maximum average rating")
            @RequestParam(required = false) Double maxAvgRating,
            @Parameter(description = "Minimum total bookings")
            @RequestParam(required = false) Integer minTotalBookings,
            @Parameter(description = "Maximum total bookings")
            @RequestParam(required = false) Integer maxTotalBookings,
            @Parameter(description = "Minimum completed bookings")
            @RequestParam(required = false) Integer minCompletedBookings,
            @Parameter(description = "Maximum completed bookings")
            @RequestParam(required = false) Integer maxCompletedBookings,
            @Parameter(description = "Minimum cancelled by seer")
            @RequestParam(required = false) Integer minCancelledBySeer,
            @Parameter(description = "Maximum cancelled by seer")
            @RequestParam(required = false) Integer maxCancelledBySeer,
            @Parameter(description = "Minimum total revenue")
            @RequestParam(required = false) BigDecimal minTotalRevenue,
            @Parameter(description = "Maximum total revenue")
            @RequestParam(required = false) BigDecimal maxTotalRevenue,
            @Parameter(description = "Minimum bonus")
            @RequestParam(required = false) BigDecimal minBonus,
            @Parameter(description = "Maximum bonus")
            @RequestParam(required = false) BigDecimal maxBonus
    ) {
        log.info("Get all seer performance - page: {}, limit: {}, month: {}, year: {}", page, limit, month, year);
        Pageable pageable = createPageable(page, limit, sortType, sortBy);
        return responseFactory.successPage(
                simpleMapper.mapToPage(
                        reportService.getAllSeerPerformance(
                                pageable, month, year,
                                minPerformancePoint, maxPerformancePoint,
                                performanceTier,
                                minRanking, maxRanking,
                                minTotalPackages, maxTotalPackages,
                                minTotalRates, maxTotalRates,
                                minAvgRating, maxAvgRating,
                                minTotalBookings, maxTotalBookings,
                                minCompletedBookings, maxCompletedBookings,
                                minCancelledBySeer, maxCancelledBySeer,
                                minTotalRevenue, maxTotalRevenue,
                                minBonus, maxBonus
                        ),
                        SeerPerformanceDto.class
                ),
                "Seer performance reports retrieved successfully"
        );
    }

    @GetMapping("/finance-statistic")
    @Operation(
            summary = "Get finance statistics",
            description = "Returns financial statistics including total revenue, net revenue after tax (7%), " +
                    "total tax, and percentage changes compared to previous month. " +
                    "Data is calculated for the current month.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<?> getFinanceStatistic() {
        return responseFactory.successSingle(
                reportService.getFinanceStatistic(),
                "Finance statistics retrieved successfully"
        );
    }

    @GetMapping("/chart")
    @Operation(
            summary = "Get chart data by chart type and time period",
            description = "Returns chart data based on chart type. " +
                    "If month and year provided: returns daily data for that month. " +
                    "If only year provided: returns monthly data for that year. " +
                    "If neither provided: returns yearly data from 2023 to current year.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = SingleResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE
                            )
                    )
            }
    )
    public ResponseEntity<?> getChart(
            @Parameter(description = "Chart type (TOTAL_REVENUE, TOTAL_BOOKING_REQUESTS, TOTAL_BOOKING_COMPLETED, TOTAL_PACKAGES)")
            @RequestParam Constants.ChartTypeDto chartType,
            @Parameter(description = "Month (1-12, optional)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Year (optional)")
            @RequestParam(required = false) Integer year
    ) {
        return responseFactory.successSingle(
                reportService.getChartDto(chartType, month, year),
                "Chart data retrieved successfully"
        );
    }
}

