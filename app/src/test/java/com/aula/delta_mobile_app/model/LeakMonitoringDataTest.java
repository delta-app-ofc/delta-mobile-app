package com.aula.delta_mobile_app.model;

import static org.junit.Assert.*;

import com.aula.delta_mobile_app.model.ApiModels.DeviceResponse;
import com.aula.delta_mobile_app.model.LeakMonitoringData.*;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

public class LeakMonitoringDataTest {
    private DeviceResponse device(int property, String id, Boolean active) {
        DeviceResponse value = new DeviceResponse();
        value.propertyId = property;
        value.deviceId = id;
        value.isActive = active;
        return value;
    }

    private Event event(String id, int property, String device, Status status) {
        return new Event(id, property, device, status, BigDecimal.ZERO, null, null, 0, 6 * 60 * 60 * 1000L, null, Collections.emptyList());
    }

    @Test
    public void noPropertyDoesNotProduceInventedEvents() {
        LeakMonitoringData value = LeakMonitoringData.unavailable(null, null, Collections.emptyList());
        assertEquals(Availability.NO_PROPERTY, value.availability);
        assertTrue(value.events.isEmpty());
        assertTrue(value.devices.isEmpty());
    }

    @Test
    public void onlyActiveDevicesOfTheAuthorizedPropertyAreUsed() {
        List<DeviceResponse> values = Arrays.asList(device(1, "owned", true), device(2, "other", true), device(1, "inactive", false), device(1, "unknown", null), null);
        LeakMonitoringData value = LeakMonitoringData.unavailable(1, "Casa", values);
        assertEquals(1, value.devices.size());
        assertEquals("owned", value.devices.get(0).deviceId);
        assertEquals(Availability.UNAVAILABLE, value.availability);
        assertTrue(value.events.isEmpty());
    }

    @Test
    public void noActiveMeterHasItsOwnState() {
        LeakMonitoringData value = LeakMonitoringData.unavailable(1, "Casa", Arrays.asList(device(1, "off", false)));
        assertEquals(Availability.NO_METER, value.availability);
    }

    @Test
    public void eventsFromOtherPropertiesOrDevicesAndDuplicatesAreRejected() {
        List<Event> events = Arrays.asList(event("a", 1, "mine", Status.ACTIVE), event("b", 2, "mine", Status.ACTIVE), event("c", 1, "other", Status.INVESTIGATING),
            event("a", 1, "mine", Status.RESOLVED), null);
        LeakMonitoringData value = LeakMonitoringData.monitored(1, "Casa", Arrays.asList(device(1, "mine", true)), events);
        assertEquals(Availability.AVAILABLE, value.availability);
        assertEquals(1, value.events.size());
        assertEquals("a", value.events.get(0).id);
        assertEquals(Status.ACTIVE, value.events.get(0).status);
    }

    @Test
    public void successfulTelemetryWithNoEventsIsDifferentFromUnavailableTelemetry() {
        List<DeviceResponse> devices = Arrays.asList(device(1, "mine", true));
        assertEquals(Availability.AVAILABLE, LeakMonitoringData.monitored(1, "Casa", devices, Collections.emptyList()).availability);
        assertEquals(Availability.UNAVAILABLE, LeakMonitoringData.unavailable(1, "Casa", devices).availability);
    }

    @Test
    public void realZeroFlowIsRetainedAndMissingVolumeIsNotMadeUp() {
        Event value = event("a", 1, "mine", Status.INVESTIGATING);
        assertEquals(BigDecimal.ZERO, value.currentFlow);
        assertNull(value.normalFlow);
        assertNull(value.volumeLiters);
    }

    @Test
    public void invalidMeasurementsAreGapsInsteadOfZero() {
        Sample negative = new Sample(100, new BigDecimal("-1"));
        Sample missing = new Sample(200, null);
        Sample zero = new Sample(300, BigDecimal.ZERO);
        Sample huge = new Sample(400, new BigDecimal("1e1000"));
        assertNull(negative.litersPerMinute);
        assertNull(missing.litersPerMinute);
        assertNull(huge.litersPerMinute);
        assertEquals(BigDecimal.ZERO, zero.litersPerMinute);
    }

    @Test
    public void timestampsAreSortedAndExplicitGapsRemainInTheSeries() {
        Event value = new Event("a", 1, "mine", Status.ACTIVE, null, null, null, 0, 6000, null, Arrays.asList(new Sample(5000, BigDecimal.ONE), new Sample(1000, BigDecimal.ZERO),
            new Sample(3000, null)));
        assertEquals(1000, value.samples.get(0).timestamp);
        assertEquals(3000, value.samples.get(1).timestamp);
        assertNull(value.samples.get(1).litersPerMinute);
        assertEquals(5000, value.samples.get(2).timestamp);
    }
}
