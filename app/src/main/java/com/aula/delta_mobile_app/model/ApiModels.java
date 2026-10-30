package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}

    public static class UpdateUserPropertyRequest {
        public Integer propertyId;
    }

    public static class UserPropertyResponse {
        public Integer id;
        public Integer userId;
        public Integer propertyId;
        public String propertyName;
        public String associationDate;
    }

    public static class UpdatePropertyRequest {
        public String name;
        public String type;
        public String classification;
        public Integer addressId;
        public Integer organizationId;
        public BigDecimal builtAreaM2;
    }

    public static class PropertyResponse {
        public Integer id;
        public String name;
        public String type;
        public String classification;
        public Integer addressId;
        public Integer organizationId;
        public BigDecimal builtAreaM2;
        public String registrationDate;
    }

    public static class UpdateOrganizationRequest {
        public String corporateName;
        public String tradeName;
        public String cnpj;
        public String businessSegment;
        public Integer declaredUnitCount;
    }

    public static class OrganizationResponse {
        public Integer id;
        public String corporateName;
        public String tradeName;
        public String cnpj;
        public String businessSegment;
        public Integer declaredUnitCount;
        public String registrationDate;
    }

    public static class UpdateLastWaterBillRequest {
        public Integer userId;
        public String month;
        public BigDecimal totalValue;
        public BigDecimal m3Value;
    }

    public static class LastWaterBillResponse {
        public Integer id;
        public Integer userId;
        public String month;
        public BigDecimal totalValue;
        public BigDecimal m3Value;
    }

    public static class UpdateUserHabitRequest {
        public Integer habitId;
        public Integer frequency;
        public List<Integer> daysOfWeek;
        public List<UserHabitScheduleResponse> schedule;
        public String effectiveFrom;
    }

    public static class UserHabitResponse {
        public Integer id;
        public Integer userId;
        public Integer habitId;
        public String habitName;
        public String habitCategory;
        public String habitDescription;
        public Integer frequency;
        public List<Integer> daysOfWeek;
        public String startTime;
        public String endTime;
        public BigDecimal estimatedLiters;
        public List<UserHabitScheduleResponse> schedule;
        public String effectiveFrom;
        public List<UserHabitScheduleVersionResponse> scheduleVersions;
    }

    public static class UserHabitScheduleResponse {
        public Integer dayOfWeek;
        public List<String> startTimes;
    }

    public static class UserHabitScheduleVersionResponse {
        public String effectiveFrom;
        public String effectiveUntil;
        public Integer frequency;
        public List<Integer> daysOfWeek;
        public List<UserHabitScheduleResponse> schedule;
    }

    public static class UpdateDeviceRequest {
        public String deviceId;
        public Integer propertyId;
        public Boolean isActive;
    }

    public static class DeviceResponse {
        public Integer id;
        public String deviceId;
        public Integer propertyId;
        public Boolean isActive;
        public String installationDate;
    }

    public static class UpdateAddressRequest {
        public Integer regionId;
        public String cep;
        public String city;
        public String state;
        public BigDecimal latitude;
        public BigDecimal longitude;
    }

    public static class AddressResponse {
        public Integer id;
        public Integer regionId;
        public String cep;
        public String city;
        public String state;
        public BigDecimal latitude;
        public BigDecimal longitude;
    }

    public static class CreateUserPropertyRequest {
        public Integer propertyId;
    }

    public static class CreatePropertyRequest {
        public Integer userId;
        public String name;
        public String type;
        public String classification;
        public Integer addressId;
        public Integer organizationId;
        public BigDecimal builtAreaM2;
    }

    public static class CreateOrganizationRequest {
        public String corporateName;
        public String tradeName;
        public String cnpj;
        public String businessSegment;
        public Integer declaredUnitCount;
    }

    public static class CreateLastWaterBillRequest {
        public Integer userId;
        public String month;
        public BigDecimal totalValue;
        public BigDecimal m3Value;
    }

    public static class CreateUserHabitRequest {
        public Integer habitId;
        public Integer frequency;
        public List<Integer> daysOfWeek;
        public List<UserHabitScheduleResponse> schedule;
    }

    public static class CreateDeviceRequest {
        public String deviceId;
        public Integer propertyId;
        public Boolean isActive;
    }

    public static class CreateAddressRequest {
        public Integer regionId;
        public String cep;
        public String city;
        public String state;
        public BigDecimal latitude;
        public BigDecimal longitude;
    }

    public static class RegionRateResponse {
        public Integer id;
        public String regionName;
        public BigDecimal m3Value;
        public String initialValidity;
        public String finalValidity;
    }

    public static class WaterCostResponse {
        public Integer propertyId;
        public Integer regionId;
        public BigDecimal consumptionM3;
        public BigDecimal ratePerM3;
        public BigDecimal estimatedCost;
        public String referenceDate;
    }

    public static class ResidentialEfficiencyRankingResponse {
        public Integer userId;
        public String personName;
        public String referenceMonth;
        public BigDecimal m3Value;
        public BigDecimal totalValue;
        public Long rankEfficiency;
        public Double consumptionPercentRank;
    }

    public static class PropertyRankingResponse {
        public Integer propertyId;
        public String propertyName;
        public BigDecimal totalLiters;
        public BigDecimal totalCost;
        public BigDecimal litersPerM2;
        public Long rankEfficiency;
        public Long rankCost;
        public Long consumptionQuartile;
        public Double consumptionPercentRank;
    }

    public static class MonthlyVariationResponse {
        public Integer propertyId;
        public String propertyName;
        public String referenceMonth;
        public BigDecimal totalLiters;
        public BigDecimal previousMonthLiters;
        public BigDecimal variationPct;
    }

    public static class ConsumptionDistributionResponse {
        public Integer propertyKey;
        public BigDecimal totalLiters;
        public Long distributionBucket;
        public BigDecimal minLiters;
        public Double q1Liters;
        public Double medianLiters;
        public Double q3Liters;
        public BigDecimal maxLiters;
    }

    public static class ConsumptionDailyResponse {
        public Integer propertyId;
        public String propertyName;
        public String fullDate;
        public BigDecimal totalLiters;
        public BigDecimal costValue;
        public BigDecimal runningTotalLiters;
        public BigDecimal movingAvg7dLiters;
    }

    public static class CapexComparisonResponse {
        public Integer scenarioId;
        public String name;
        public BigDecimal investmentValue;
        public BigDecimal reductionPct;
        public BigDecimal annualSavingsValue;
        public BigDecimal paybackMonths;
        public Long rankByPayback;
    }

    public static class AuditHistoryChainResponse {
        public Integer logId;
        public Integer regionRateId;
        public BigDecimal m3Value;
        public String operation;
        public String executedBy;
        public String executedAt;
        public Integer level;
    }
}
