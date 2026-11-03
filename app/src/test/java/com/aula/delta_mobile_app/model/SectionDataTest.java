package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.*;

import org.junit.Test;

import java.util.*;

public class SectionDataTest {
    @Test
    public void propertiesExcludeOtherUsersAndInvalidLinks() {
        UserPropertyResponse own = property(1, 10), other = property(2, 20), invalid = property(1, null);
        assertEquals(Collections.singletonList(own), SectionData.properties(Arrays.asList(null, other, own, invalid, property(1, 0)), 1));
        assertTrue(SectionData.properties(Arrays.asList(own), null).isEmpty());
    }

    @Test
    public void propertySelectionSurvivesReloadAndReplacesRevokedLinks() {
        List<UserPropertyResponse> properties = Arrays.asList(property(1, 10), property(1, 20));
        assertEquals(Integer.valueOf(20), SectionData.selectedProperty(properties, 20));
        assertEquals(Integer.valueOf(10), SectionData.selectedProperty(properties, 99));
        assertEquals(Integer.valueOf(10), SectionData.selectedProperty(properties, null));
        assertNull(SectionData.selectedProperty(Collections.emptyList(), 10));
    }

    private UserPropertyResponse property(Integer user, Integer property) {
        UserPropertyResponse result = new UserPropertyResponse();
        result.userId = user;
        result.propertyId = property;
        return result;
    }

    @Test
    public void billsNeverExposeAnotherUsersRecordsAndSortNewestFirst() {
        LastWaterBillResponse old = bill(1, "2026-08-01"), recent = bill(1, "2026-10-01"), other = bill(2, "2026-11-01");
        List<LastWaterBillResponse> source = Arrays.asList(old, null, other, recent);
        assertEquals(Arrays.asList(recent, old), SectionData.bills(source, 1));
        assertEquals(4, source.size());
    }

    @Test
    public void unknownUserNeverMatchesUnownedRecords() {
        assertTrue(SectionData.bills(Collections.singletonList(bill(null, null)), null).isEmpty());
        UserHabitResponse orphan = new UserHabitResponse();
        assertTrue(SectionData.habits(Collections.singletonList(orphan), null).isEmpty());
    }

    @Test
    public void habitsAreScopedToCurrentUser() {
        UserHabitResponse own = new UserHabitResponse();
        own.userId = 1;
        UserHabitResponse other = new UserHabitResponse();
        other.userId = 2;
        assertEquals(Collections.singletonList(own), SectionData.habits(Arrays.asList(null, other, own), 1));
    }

    @Test
    public void emptyResponsesAndMissingMonthsAreSafe() {
        assertTrue(SectionData.bills(null, 1).isEmpty());
        assertTrue(SectionData.habits(null, 1).isEmpty());
        LastWaterBillResponse unknown = bill(1, null), dated = bill(1, "2026-10-01");
        assertEquals(Arrays.asList(dated, unknown), SectionData.bills(Arrays.asList(unknown, dated), 1));
    }

    private LastWaterBillResponse bill(Integer user, String month) {
        LastWaterBillResponse result = new LastWaterBillResponse();
        result.userId = user;
        result.month = month;
        return result;
    }

}
