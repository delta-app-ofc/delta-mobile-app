package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.*;

import org.junit.Test;

import java.util.*;

public class SectionDataTest {
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

    @Test
    public void rankingIncludesOnlyOwnedPropertiesWithKnownIds() {
        PropertyRankingResponse own = new PropertyRankingResponse();
        own.propertyId = 10;
        own.rankEfficiency = 2L;
        PropertyRankingResponse other = new PropertyRankingResponse();
        other.propertyId = 20;
        other.rankEfficiency = 1L;
        PropertyRankingResponse unknown = new PropertyRankingResponse();
        assertEquals(Collections.singletonList(own), SectionData.rankings(Arrays.asList(other, null, own, unknown), Collections.singleton(10)));
    }

    @Test
    public void rankingSortsByServerPositionAndKeepsMissingRanksLast() {
        PropertyRankingResponse first = new PropertyRankingResponse();
        first.propertyId = 10;
        first.rankEfficiency = 1L;
        PropertyRankingResponse pending = new PropertyRankingResponse();
        pending.propertyId = 11;
        assertEquals(Arrays.asList(first, pending), SectionData.rankings(Arrays.asList(pending, first), new HashSet<>(Arrays.asList(10, 11))));
        assertTrue(SectionData.rankings(Arrays.asList(first), null).isEmpty());
    }
}
