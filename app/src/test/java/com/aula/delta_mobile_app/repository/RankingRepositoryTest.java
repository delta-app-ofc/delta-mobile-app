package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.ApiModels.PropertyRankingResponse;
import com.aula.delta_mobile_app.model.ApiModels.UserPropertyResponse;
import com.aula.delta_mobile_app.model.AuthModels.Profile;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.ApiService;
import com.aula.delta_mobile_app.services.network.GsonProvider;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import static org.junit.Assert.*;

public class RankingRepositoryTest {
    private final Map<String, Response<?>> responses = new HashMap<>();
    private final List<String> requests = new ArrayList<>();

    private RankingRepository repository(List<UserPropertyResponse> links, List<PropertyRankingResponse> rankings) {
        Profile profile = new Profile();
        profile.id = 7;
        responses.put("me", Response.success(profile));
        responses.put("listUserProperties", Response.success(links));
        responses.put("propertyRanking", Response.success(rankings));
        ApiService api = (ApiService) Proxy.newProxyInstance(ApiService.class.getClassLoader(), new Class<?>[] { ApiService.class }, (proxy, method, args) -> {
            requests.add(method.getName());
            if (method.getName().equals("listUserProperties")) assertEquals(7, args[0]);
            Response<?> response = responses.get(method.getName());
            assertNotNull(response);
            return Proxy.newProxyInstance(Call.class.getClassLoader(), new Class<?>[] { Call.class }, (call, operation, values) -> {
                if (!operation.getName().equals("execute")) throw new AssertionError(operation.getName());
                return response;
            });
        });
        return new RankingRepository(api);
    }

    private UserPropertyResponse link(int user, int property) {
        UserPropertyResponse link = new UserPropertyResponse();
        link.userId = user;
        link.propertyId = property;
        return link;
    }

    private PropertyRankingResponse row(Integer property, Long position) {
        PropertyRankingResponse row = new PropertyRankingResponse();
        row.propertyId = property;
        row.rankEfficiency = position;
        return row;
    }

    @Test
    public void filtersOwnershipAndSortsWithoutRenumberingServerPositions() throws Exception {
        PropertyRankingResponse first = row(10, 3L), second = row(20, 8L), pending = row(30, null);
        List<PropertyRankingResponse> source = Arrays.asList(pending, row(99, 1L), null, second, row(null, 2L), first);
        RankingRepository repository = repository(Arrays.asList(link(7, 10), link(7, 20), link(7, 30), link(8, 99), null), source);
        ScreenState<List<PropertyRankingResponse>> result = repository.load();
        assertEquals(ScreenState.Status.SUCCESS, result.status);
        assertEquals(Arrays.asList(first, second, pending), result.data);
        assertEquals(Long.valueOf(3), result.data.get(0).rankEfficiency);
        assertNull(result.data.get(2).totalLiters);
        assertEquals(6, source.size());
        assertEquals(Arrays.asList("me", "listUserProperties", "propertyRanking"), requests);
    }

    @Test
    public void noOwnedPropertiesReturnsEmptyWithoutRequestingRanking() throws Exception {
        ScreenState<List<PropertyRankingResponse>> result = repository(Arrays.asList(link(8, 10), link(7, 0)), Collections.emptyList()).load();
        assertEquals(ScreenState.Status.SUCCESS, result.status);
        assertTrue(result.data.isEmpty());
        assertFalse(requests.contains("propertyRanking"));
    }

    @Test
    public void emptyRankingIsSuccessButMissingBodyIsFailure() throws Exception {
        RankingRepository repository = repository(Collections.singletonList(link(7, 10)), Collections.emptyList());
        assertEquals(ScreenState.Status.SUCCESS, repository.load().status);
        responses.put("propertyRanking", Response.success(null));
        ScreenState<List<PropertyRankingResponse>> result = repository.load();
        assertEquals(ScreenState.Status.ERROR, result.status);
        assertFalse(result.message.isEmpty());
    }

    @Test
    public void invalidIdentityNeverRequestsPropertyData() throws Exception {
        RankingRepository repository = repository(Collections.emptyList(), Collections.emptyList());
        responses.put("me", Response.success(new Profile()));
        assertEquals(ScreenState.Status.ERROR, repository.load().status);
        assertEquals(Collections.singletonList("me"), requests);
    }

    @Test
    public void missingLinksAreAnErrorRatherThanAnEmptyRanking() throws Exception {
        RankingRepository repository = repository(null, Collections.emptyList());
        assertEquals(ScreenState.Status.ERROR, repository.load().status);
        assertFalse(requests.contains("propertyRanking"));
    }

    @Test
    public void httpErrorsAndRetryDeadlineArePreserved() throws Exception {
        RankingRepository repository = repository(Collections.singletonList(link(7, 10)), Collections.emptyList());
        responses.put("propertyRanking", Response.error(429, ResponseBody.create(MediaType.get("application/json"), "{}")));
        ScreenState<List<PropertyRankingResponse>> result = repository.load();
        assertEquals(ScreenState.Status.ERROR, result.status);
        assertEquals(429, result.code);
        assertTrue(result.retryAt > System.currentTimeMillis());
        responses.put("me", Response.error(403, ResponseBody.create(MediaType.get("application/json"), "{}")));
        assertEquals(403, repository.load().code);
    }

    @Test
    public void routeAndDecimalFieldsMatchPublishedContract() throws Exception {
        assertEquals("delta/analytics/property-ranking", ApiService.class.getMethod("propertyRanking").getAnnotation(GET.class).value());
        PropertyRankingResponse row = GsonProvider.getInstance().fromJson("{\"propertyId\":10,\"totalLiters\":1234.50,\"totalCost\":45.67,\"litersPerM2\":12.345,\"rankEfficiency\":3,\"rankCost\":8,\"consumptionQuartile\":2,\"consumptionPercentRank\":0.25}", PropertyRankingResponse.class);
        assertEquals(new BigDecimal("45.67"), row.totalCost);
        assertEquals(new BigDecimal("1234.50"), row.totalLiters);
        assertEquals(Long.valueOf(8), row.rankCost);
        assertEquals(Long.valueOf(2), row.consumptionQuartile);
        assertEquals(Double.valueOf(0.25), row.consumptionPercentRank);
    }
}
