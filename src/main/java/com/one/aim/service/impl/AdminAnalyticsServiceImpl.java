package com.one.aim.service.impl;

import com.one.aim.bo.SellerBO;
import com.one.aim.bo.UserActivityBO;
import com.one.aim.mapper.AdminAnalyticsMapper;
import com.one.aim.repo.*;
import com.one.aim.rq.CustomReportRq;
import com.one.aim.rs.*;
import com.one.aim.service.AdminAnalyticsService;
import com.one.vm.core.BaseRs;
import com.one.vm.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final OrderItemBORepo orderItemRepo;
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final UserActivityRepo userActivityRepo;
    private final SellerRepo sellerRepo;
    private final AdminAnalyticsMapper mapper;
    private final MarketingConversionRepo marketingConversionRepo;

    // ================= UTIL =================
    private LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOf(LocalDate date) {
        return date.atTime(23, 59, 59);
    }

    private LocalDateTime[] resolveRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            LocalDateTime now = LocalDateTime.now();
            return new LocalDateTime[]{now.minusDays(30), now};
        }
        return new LocalDateTime[]{startOf(start), endOf(end)};
    }

    @Override
    public AdminAnalyticsRs getDashboard() {

        // DEFAULT RANGE = last 30 days
        LocalDate startDate = null;
        LocalDate endDate = null;

        // DEFAULT PAGINATION
        int page = 0;
        int size = 5;

        SummaryCardsRs summary =
                getOverview(startDate, endDate);

        SalesPerformanceRs salesPerformance =
                getSalesChart(startDate, endDate);

        UserActivityRs userActivity =
                getUserActivityChart(startDate, endDate);

        Page<SalesTableRowRs> salesPage =
                getSalesPerformanceReport(startDate, endDate, page, size);

        return mapper.toAdminAnalyticsResponse(
                summary,
                salesPerformance,
                userActivity,
                salesPage.getContent()
        );
    }



    // ================= OVERVIEW =================
    @Override
    public SummaryCardsRs getOverview(LocalDate startDate, LocalDate endDate) {
        LocalDateTime[] range = resolveRange(startDate, endDate);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        Long revenue = orderItemRepo.getTotalRevenue(start, end);
        Long orders = orderRepo.getOrderVolume(start, end);
        Long users = userRepo.countNewUsers(start, end);

        // ✅ FIX: Calculate sales trend for SAME DURATION in previous period
        long days = ChronoUnit.DAYS.between(start, end);
        LocalDateTime prevStart = start.minusDays(days + 1); // +1 to avoid overlap
        LocalDateTime prevEnd = start.minusSeconds(1); // End just before current period

        Long prevRevenue = orderItemRepo.getTotalRevenue(prevStart, prevEnd);
        int salesTrend = calculatePercentChange(prevRevenue, revenue);

        // ✅ FIX: Get ACTUAL top selling product by name
        List<Object[]> topList = orderItemRepo.getTopSellingProduct(start, end);
        Object[] top1 = topList.isEmpty() ? null : topList.get(0);
        String topProduct = "N/A";

        if (topList != null && !topList.isEmpty() && topList.get(0) != null) {
            Object[] row = topList.get(0);
            // Assuming row[0] is product name and row[1] is quantity/revenue
            topProduct = row[0] != null ? row[0].toString() : "N/A";
        }

        return mapper.toSummaryCards(
                revenue == null ? 0 : revenue,
                salesTrend,
                users == null ? 0 : users,
                orders == null ? 0 : orders,
                topProduct
        );
    }

    // ================= SALES CHART WITH % CHANGE =================
    @Override
    public SalesPerformanceRs getSalesChart(LocalDate startDate, LocalDate endDate) {
        var r = resolveRange(startDate, endDate);
        LocalDateTime start = r[0];
        LocalDateTime end = r[1];

        // Current period total
        Long currentTotal = orderItemRepo.getTotalRevenue(start, end);

        // Previous period (same duration, shifted back)
        long days = ChronoUnit.DAYS.between(start, end);
        LocalDateTime prevStart = start.minusDays(days);
        LocalDateTime prevEnd = start;
        Long previousTotal = orderItemRepo.getTotalRevenue(prevStart, prevEnd);

        // Calculate percentage change
        int percentChange = calculatePercentChange(previousTotal, currentTotal);

        // Map weekly data
        List<WeeklyRevenueRs> weekly = generateWeeklyRevenue(start, end)
                .stream()
                .map(w -> WeeklyRevenueRs.builder()
                        .weekLabel(w.getLabel())
                        .revenue(w.getValue())
                        .build())
                .toList();

        return SalesPerformanceRs.builder()
                .totalSalesLast30Days(currentTotal == null ? 0 : currentTotal)
                .percentChange(percentChange)
                .weeklyRevenue(weekly)
                .build();
    }

    // ================= USER ACTIVITY CHART WITH % CHANGE =================
    @Override
    public UserActivityRs getUserActivityChart(LocalDate startDate, LocalDate endDate) {
        var r = resolveRange(startDate, endDate);
        LocalDateTime start = r[0];
        LocalDateTime end = r[1];

        // Current period active users
        Long currentActive = orderRepo.getActiveUsers(start, end);

        // Previous period (same duration, shifted back)
        long days = ChronoUnit.DAYS.between(start, end);
        LocalDateTime prevStart = start.minusDays(days);
        LocalDateTime prevEnd = start;
        Long previousActive = orderRepo.getActiveUsers(prevStart, prevEnd);

        // Calculate percentage change
        int percentChange = calculatePercentChange(previousActive, currentActive);

        // Map weekly data
        List<WeeklyUserActivityRs> weekly = generateWeeklyActivity(start, end)
                .stream()
                .map(w -> WeeklyUserActivityRs.builder()
                        .weekLabel(w.getLabel())
                        .userCount(w.getValue())
                        .build())
                .toList();

        return UserActivityRs.builder()
                .totalActiveUsers(currentActive == null ? 0 : currentActive)
                .percentChange(percentChange)
                .weeklyActivity(weekly)
                .build();
    }

    // ================= HELPER: Calculate % Change =================
    private int calculatePercentChange(Long previous, Long current) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? 100 : 0;
        }
        if (current == null) {
            return -100;
        }

        double change = ((double) (current - previous) / previous) * 100;
        return (int) Math.round(change);
    }


    // ================= SALES TABLE =================
    @Override
    public Page<SalesTableRowRs> getSalesPerformanceReport(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        LocalDateTime[] range = resolveRange(startDate, endDate);
        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> rows =
                orderItemRepo.getSalesTable(range[0], range[1], pageable);

        return rows.map(row -> {
            Long sellerId = ((Number) row[2]).longValue();
            return mapper.toSalesTableRow(
                    row,
                    sellerRepo.findById(sellerId).orElse(null)
            );
        });
    }

    // ================= USER ACTIVITY TABLE =================
    @Override
    public Page<UserActivityRowRs> getUserActivityReport(
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        LocalDateTime[] range = resolveRange(startDate, endDate);
        Pageable pageable = PageRequest.of(page, size);

        return userActivityRepo
                .findByCreatedAtBetween(range[0], range[1], pageable)
                .map(mapper::toUserActivityRow);
    }

    @Override
    public Page<CustomReportRowRs> generateCustomReport(CustomReportRq rq) {

        LocalDateTime[] range = resolveRange(
                rq.getStartDate(),
                rq.getEndDate()
        );

        Pageable pageable = PageRequest.of(
                rq.getPage(),
                rq.getSize()
        );

        Page<Object[]> rows = orderItemRepo.getCustomReport(
                range[0],
                range[1],
                rq.getCategory(),
                rq.getSellerId(),
                pageable
        );

        return rows.map(row -> CustomReportRowRs.builder()
                .label((String) row[0])
                .revenue(rq.isRevenue() ? ((Number) row[1]).longValue() : null)
                .orders(rq.isOrders() ? ((Number) row[2]).longValue() : null)
                .users(rq.isUsers() ? ((Number) row[3]).longValue() : null)
                .build()
        );
    }

    @Override
    public Page<?> getCustomReport(
            String type,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        LocalDateTime[] range = resolveRange(startDate, endDate);
        Pageable pageable = PageRequest.of(page, size);

        return switch (type.toLowerCase()) {

            case "sales" ->
                    getSalesPerformanceReport(startDate, endDate, page, size);

            case "user" ->
                    getUserActivityReport(startDate, endDate, page, size);

            case "marketing" ->
                    marketingConversionRepo
                            .getCampaignAggregates(range[0], range[1], pageable)
                            .map(row -> mapper.toMarketingEffectivenessRow(
                                    (String) row[0],
                                    ((Number) row[1]).longValue(),
                                    ((Number) row[2]).longValue(),
                                    ((Number) row[3]).doubleValue(),
                                    (String) row[4]
                            ));

            default ->
                    throw new IllegalArgumentException("Invalid report type");
        };
    }


    @Override
    public byte[] exportCustomReport(CustomReportRq rq) {

        Page<CustomReportRowRs> page =
                generateCustomReport(rq);

        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Label,Revenue,Orders,Users\n");

        for (CustomReportRowRs row : page.getContent()) {
            csv.append(row.getLabel()).append(",");
            csv.append(row.getRevenue() != null ? row.getRevenue() : "").append(",");
            csv.append(row.getOrders() != null ? row.getOrders() : "").append(",");
            csv.append(row.getUsers() != null ? row.getUsers() : "").append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ================= HELPER: Generate Weekly Data Points =================
    private List<WeeklyDataPoint> generateWeeklyRevenue(LocalDateTime start, LocalDateTime end) {
        List<WeeklyDataPoint> weeklyData = new ArrayList<>();

        long totalDays = ChronoUnit.DAYS.between(start, end);

        // ✅ Determine appropriate grouping
        int weeks;
        int daysPerWeek;

        if (totalDays <= 7) {
            // Show daily data for ranges <= 1 week
            weeks = (int) totalDays;
            daysPerWeek = 1;
        } else if (totalDays <= 30) {
            // Show weekly data for ranges <= 1 month
            weeks = (int) Math.ceil(totalDays / 7.0);
            daysPerWeek = 7;
        } else {
            // Show bi-weekly or monthly for longer ranges
            weeks = (int) Math.ceil(totalDays / 14.0);
            daysPerWeek = 14;
        }

        LocalDateTime currentStart = start;

        for (int i = 0; i < weeks; i++) {
            LocalDateTime periodEnd = currentStart.plusDays(daysPerWeek);
            if (periodEnd.isAfter(end)) {
                periodEnd = end;
            }

            // ✅ Get revenue for this period
            Long revenue = orderItemRepo.getTotalRevenue(currentStart, periodEnd);

            // ✅ Better labels
            String label;
            if (daysPerWeek == 1) {
                label = currentStart.format(DateTimeFormatter.ofPattern("MMM dd"));
            } else if (daysPerWeek == 7) {
                label = String.format("Week %d", i + 1);
            } else {
                label = String.format("Period %d", i + 1);
            }

            weeklyData.add(new WeeklyDataPoint(label, revenue != null ? revenue : 0L));

            currentStart = periodEnd;
            if (!currentStart.isBefore(end)) break;
        }

        return weeklyData;
    }

    // ✅ Apply same logic to generateWeeklyActivity
    private List<WeeklyDataPoint> generateWeeklyActivity(LocalDateTime start, LocalDateTime end) {
        List<WeeklyDataPoint> weeklyData = new ArrayList<>();

        long totalDays = ChronoUnit.DAYS.between(start, end);

        int weeks;
        int daysPerWeek;

        if (totalDays <= 7) {
            weeks = (int) totalDays;
            daysPerWeek = 1;
        } else if (totalDays <= 30) {
            weeks = (int) Math.ceil(totalDays / 7.0);
            daysPerWeek = 7;
        } else {
            weeks = (int) Math.ceil(totalDays / 14.0);
            daysPerWeek = 14;
        }

        LocalDateTime currentStart = start;

        for (int i = 0; i < weeks; i++) {
            LocalDateTime periodEnd = currentStart.plusDays(daysPerWeek);
            if (periodEnd.isAfter(end)) {
                periodEnd = end;
            }

            // ✅ Count active users (who logged in)
            Long activeUsers = userActivityRepo.getWeeklyActiveUsersByType(currentStart, periodEnd)
                    .stream()
                    .mapToLong(r -> ((Number) r[1]).longValue())
                    .sum();



            String label;
            if (daysPerWeek == 1) {
                label = currentStart.format(DateTimeFormatter.ofPattern("MMM dd"));
            } else if (daysPerWeek == 7) {
                label = String.format("Week %d", i + 1);
            } else {
                label = String.format("Period %d", i + 1);
            }

            weeklyData.add(new WeeklyDataPoint(label, activeUsers != null ? activeUsers : 0L));

            currentStart = periodEnd;
            if (!currentStart.isBefore(end)) break;
        }

        return weeklyData;
    }

}
