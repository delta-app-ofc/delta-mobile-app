package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.CreateUserHabitRequest;
import com.aula.delta_mobile_app.model.ApiModels.UpdateUserHabitRequest;
import com.aula.delta_mobile_app.model.ApiModels.UserHabitResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class RoutineDataTest {
    private static final String EXPANDED = "{\"id\":105,\"userId\":42,\"habitId\":1,\"habitName\":\"Banho longo\",\"frequency\":5,"
        + "\"daysOfWeek\":[1,3],\"schedule\":[{\"dayOfWeek\":1,\"startTimes\":[\"07:00\",\"20:00\"]},"
        + "{\"dayOfWeek\":3,\"startTimes\":[\"08:00\",\"14:00\",\"21:00\"]}]}";
    private final Gson gson = new Gson();

    @Test
    public void expandedCollectionAndDetailPreserveDistributionAndIndividualTimes() {
        UserHabitResponse[] collection = gson.fromJson("[" + EXPANDED + "]", UserHabitResponse[].class);
        UserHabitResponse detail = gson.fromJson(EXPANDED, UserHabitResponse.class);
        assertEquals(Integer.valueOf(105), detail.id);
        assertEquals(Integer.valueOf(42), detail.userId);
        assertEquals(Integer.valueOf(1), detail.habitId);
        assertEquals("Banho longo", detail.habitName);
        assertEquals(Integer.valueOf(5), detail.frequency);
        assertEquals(Arrays.asList(1, 3), detail.daysOfWeek);
        assertEquals(2, detail.schedule.size());
        assertEquals(Integer.valueOf(1), detail.schedule.get(0).dayOfWeek);
        assertEquals(Arrays.asList("07:00", "20:00"), detail.schedule.get(0).startTimes);
        assertEquals(Integer.valueOf(3), detail.schedule.get(1).dayOfWeek);
        assertEquals(Arrays.asList("08:00", "14:00", "21:00"), detail.schedule.get(1).startTimes);
        assertEquals(gson.toJson(detail), gson.toJson(collection[0]));
    }

    @Test
    public void responseAlsoPreservesPublishedMetadataAndLegacyValues() {
        UserHabitResponse value = gson.fromJson("{\"habitCategory\":\"Higiene\",\"habitDescription\":\"Referencia\","
            + "\"startTime\":\"07:00:00\",\"endTime\":\"07:20:00\",\"estimatedLiters\":12.345}", UserHabitResponse.class);
        assertEquals("Higiene", value.habitCategory);
        assertEquals("Referencia", value.habitDescription);
        assertEquals("07:00:00", value.startTime);
        assertEquals("07:20:00", value.endTime);
        assertEquals(new BigDecimal("12.345"), value.estimatedLiters);
        assertNull(value.schedule);
    }

    @Test
    public void missingOrNullTransportFieldsStayAbsentRatherThanBecomingDefaults() {
        UserHabitResponse legacy = gson.fromJson("{\"id\":105,\"userId\":42,\"habitId\":1,\"frequency\":5,\"daysOfWeek\":[1,3]}", UserHabitResponse.class);
        UserHabitResponse absent = gson.fromJson("{\"schedule\":null,\"frequency\":null,\"estimatedLiters\":null}", UserHabitResponse.class);
        assertNull(legacy.schedule);
        assertNull(legacy.startTime);
        assertNull(legacy.estimatedLiters);
        assertEquals(Integer.valueOf(5), legacy.frequency);
        assertEquals(Arrays.asList(1, 3), legacy.daysOfWeek);
        assertNull(absent.schedule);
        assertNull(absent.frequency);
        assertNull(absent.estimatedLiters);
        UserHabitResponse partial = gson.fromJson("{\"schedule\":[{\"dayOfWeek\":1}]}", UserHabitResponse.class);
        assertNull(partial.schedule.get(0).startTimes);
    }

    @Test
    public void readingFieldsDoNotChangeExistingWritePayloads() {
        CreateUserHabitRequest create = new CreateUserHabitRequest();
        create.habitId = 1;
        create.frequency = 5;
        create.daysOfWeek = Arrays.asList(1, 3);
        UpdateUserHabitRequest update = new UpdateUserHabitRequest();
        update.habitId = 1;
        update.frequency = 5;
        update.daysOfWeek = Arrays.asList(1, 3);
        String expected = "{\"habitId\":1,\"frequency\":5,\"daysOfWeek\":[1,3]}";
        assertEquals(gson.fromJson(expected, Object.class), gson.fromJson(gson.toJson(create), Object.class));
        assertEquals(gson.fromJson(expected, Object.class), gson.fromJson(gson.toJson(update), Object.class));
    }

}
