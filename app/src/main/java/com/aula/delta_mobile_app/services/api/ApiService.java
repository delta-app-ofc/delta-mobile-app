package com.aula.delta_mobile_app.services.api;

import com.aula.delta_mobile_app.model.ApiModels.*;
import com.aula.delta_mobile_app.model.AuthModels.*;
import com.aula.delta_mobile_app.model.PostalCodeResponse;

import retrofit2.Call;
import retrofit2.http.*;

import java.math.BigDecimal;
import java.util.List;

public interface ApiService {
    @POST("delta/auth/create-account")
    Call<Tokens> create(@Body CreateAccount request);

    @POST("delta/auth/login")
    Call<Tokens> login(@Body Credentials request);

    @POST("delta/auth/refresh")
    Call<Tokens> refresh(@Body Refresh request);

    @GET("delta/auth/me")
    Call<Profile> me();

    @GET("delta/user-properties/{userId}")
    Call<List<PropertyLink>> properties(@Path("userId") int id);

    @POST("delta/auth/complete-registration")
    Call<Completed> complete(@Body Completion request);

    @GET("cep/v2/{cep}")
    Call<PostalCodeResponse> getCep(@Path("cep") String postalCode);

    @GET("delta/address")
    Call<List<AddressResponse>> listAddresses();

    @GET("delta/address/{id}")
    Call<AddressResponse> getAddress(@Path("id") int id);

    @POST("delta/address")
    Call<AddressResponse> createAddress(@Body CreateAddressRequest request);

    @PUT("delta/address/{id}")
    Call<AddressResponse> updateAddress(@Path("id") int id, @Body UpdateAddressRequest request);

    @DELETE("delta/address/{id}")
    Call<Void> deleteAddress(@Path("id") int id);

    @GET("delta/property")
    Call<List<PropertyResponse>> listProperties();

    @GET("delta/property/{id}")
    Call<PropertyResponse> getProperty(@Path("id") int id);

    @POST("delta/property")
    Call<PropertyResponse> createProperty(@Body CreatePropertyRequest request);

    @PUT("delta/property/{id}")
    Call<PropertyResponse> updateProperty(@Path("id") int id, @Body UpdatePropertyRequest request);

    @DELETE("delta/property/{id}")
    Call<Void> deleteProperty(@Path("id") int id);

    @GET("delta/organizations")
    Call<List<OrganizationResponse>> listOrganizations();

    @GET("delta/organizations/{id}")
    Call<OrganizationResponse> getOrganization(@Path("id") int id);

    @POST("delta/organizations")
    Call<OrganizationResponse> createOrganization(@Body CreateOrganizationRequest request);

    @PUT("delta/organizations/{id}")
    Call<OrganizationResponse> updateOrganization(
        @Path("id") int id, @Body UpdateOrganizationRequest request);

    @DELETE("delta/organizations/{id}")
    Call<Void> deleteOrganization(@Path("id") int id);

    @GET("delta/last-water-bills")
    Call<List<LastWaterBillResponse>> listLastWaterBills();

    @GET("delta/last-water-bills/{id}")
    Call<LastWaterBillResponse> getLastWaterBill(@Path("id") int id);

    @POST("delta/last-water-bills")
    Call<LastWaterBillResponse> createLastWaterBill(@Body CreateLastWaterBillRequest request);

    @PUT("delta/last-water-bills/{id}")
    Call<LastWaterBillResponse> updateLastWaterBill(
        @Path("id") int id, @Body UpdateLastWaterBillRequest request);

    @DELETE("delta/last-water-bills/{id}")
    Call<Void> deleteLastWaterBill(@Path("id") int id);

    @GET("delta/device")
    Call<List<DeviceResponse>> listDevices();

    @GET("delta/device/{id}")
    Call<DeviceResponse> getDevice(@Path("id") int id);

    @POST("delta/device")
    Call<DeviceResponse> createDevice(@Body CreateDeviceRequest request);

    @PUT("delta/device/{id}")
    Call<DeviceResponse> updateDevice(@Path("id") int id, @Body UpdateDeviceRequest request);

    @DELETE("delta/device/{id}")
    Call<Void> deleteDevice(@Path("id") int id);

    @GET("delta/user-properties/{userId}")
    Call<List<UserPropertyResponse>> listUserProperties(@Path("userId") int userId);

    @GET("delta/user-properties/{userId}/{id}")
    Call<UserPropertyResponse> getUserProperty(@Path("userId") int userId, @Path("id") int id);

    @POST("delta/user-properties/{userId}")
    Call<UserPropertyResponse> createUserProperty(
        @Path("userId") int userId, @Body CreateUserPropertyRequest request);

    @PUT("delta/user-properties/{userId}/{id}")
    Call<UserPropertyResponse> updateUserProperty(
        @Path("userId") int userId,
        @Path("id") int id,
        @Body UpdateUserPropertyRequest request);

    @DELETE("delta/user-properties/{userId}/{id}")
    Call<Void> deleteUserProperty(@Path("userId") int userId, @Path("id") int id);

    @GET("delta/habits/{userId}")
    Call<List<UserHabitResponse>> listUserHabits(@Path("userId") int userId);

    @GET("delta/habits/{userId}/{id}")
    Call<UserHabitResponse> getUserHabit(@Path("userId") int userId, @Path("id") int id);

    @POST("delta/habits/{userId}")
    Call<UserHabitResponse> createUserHabit(
        @Path("userId") int userId, @Body CreateUserHabitRequest request);

    @PUT("delta/habits/{userId}/{id}")
    Call<UserHabitResponse> updateUserHabit(
        @Path("userId") int userId, @Path("id") int id, @Body UpdateUserHabitRequest request);

    @DELETE("delta/habits/{userId}/{id}")
    Call<Void> deleteUserHabit(@Path("userId") int userId, @Path("id") int id);

    @GET("delta/region-rate/{regionId}")
    Call<List<RegionRateResponse>> regionRates(
        @Path("regionId") int regionId, @Query("last") Boolean last);

    @GET("delta/property/{propertyId}/watercost")
    Call<WaterCostResponse> waterCost(
        @Path("propertyId") int propertyId,
        @Query("consumptionM3") BigDecimal consumptionM3,
        @Query("referenceDate") String referenceDate);

    @GET("delta/analytics/consumption-daily/{propertyId}")
    Call<List<ConsumptionDailyResponse>> consumptionDaily(@Path("propertyId") int propertyId);

    @GET("delta/analytics/property-ranking")
    Call<List<PropertyRankingResponse>> propertyRanking();

    @GET("delta/analytics/monthly-variation/{propertyId}")
    Call<List<MonthlyVariationResponse>> monthlyVariation(@Path("propertyId") int propertyId);

    @GET("delta/analytics/consumption-distribution")
    Call<List<ConsumptionDistributionResponse>> consumptionDistribution();

    @GET("delta/analytics/capex-comparison")
    Call<List<CapexComparisonResponse>> capexComparison();

    @GET("delta/analytics/residential-efficiency-ranking/{userId}")
    Call<List<ResidentialEfficiencyRankingResponse>> residentialEfficiencyRanking(
        @Path("userId") int userId);

    @GET("delta/analytics/audit-history")
    Call<List<AuditHistoryChainResponse>> auditHistory(@Query("regionRateId") Integer regionRateId);
}
