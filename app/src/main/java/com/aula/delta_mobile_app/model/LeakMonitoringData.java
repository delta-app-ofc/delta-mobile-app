package com.aula.delta_mobile_app.model;

import java.math.BigDecimal;
import java.util.*;

public final class LeakMonitoringData {
    public enum Availability {
        NO_PROPERTY, NO_METER, UNAVAILABLE, AVAILABLE
    }

    public enum Status {
        ACTIVE, INVESTIGATING, RESOLVED
    }

    public static final class Sample {
        public final long timestamp;
        public final BigDecimal litersPerMinute;

        public Sample(long timestamp, BigDecimal litersPerMinute) {
            this.timestamp = timestamp;
            this.litersPerMinute = validFlow(litersPerMinute);
        }
    }

    public static final class Event {
        public final String id, deviceId, normalWindow;
        public final int propertyId;
        public final Status status;
        public final BigDecimal currentFlow, normalFlow, volumeLiters;
        public final long windowStart, windowEnd;
        public final List<Sample> samples;

        public Event(String id, int propertyId, String deviceId, Status status, BigDecimal currentFlow, BigDecimal normalFlow, BigDecimal volumeLiters, long windowStart,
            long windowEnd, String normalWindow, List<Sample> samples) {
            this.id = id;
            this.propertyId = propertyId;
            this.deviceId = deviceId;
            this.status = status;
            this.currentFlow = validFlow(currentFlow);
            this.normalFlow = validFlow(normalFlow);
            this.volumeLiters = validFlow(volumeLiters);
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
            this.normalWindow = normalWindow;
            this.samples = Collections.unmodifiableList(orderedSamples(samples));
        }
    }

    public final Integer propertyId;
    public final String propertyName;
    public final Availability availability;
    public final List<ApiModels.DeviceResponse> devices;
    public final List<Event> events;

    private LeakMonitoringData(Integer id, String name, List<ApiModels.DeviceResponse> devices, List<Event> events, Availability availability) {
        propertyId = id;
        propertyName = name;
        this.availability = availability;
        this.devices = Collections.unmodifiableList(new ArrayList<>(devices));
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
    }

    public static LeakMonitoringData unavailable(Integer id, String name, List<ApiModels.DeviceResponse> devices) {
        List<ApiModels.DeviceResponse> active = activeDevices(id, devices);
        Availability state = id == null ? Availability.NO_PROPERTY : active.isEmpty() ? Availability.NO_METER : Availability.UNAVAILABLE;
        return new LeakMonitoringData(id, name, active, Collections.emptyList(), state);
    }

    public static LeakMonitoringData monitored(int id, String name, List<ApiModels.DeviceResponse> devices, List<Event> events) {
        List<ApiModels.DeviceResponse> active = activeDevices(id, devices);
        Set<String> ids = new HashSet<>();
        for (ApiModels.DeviceResponse device : active)
            if (device.deviceId != null) ids.add(device.deviceId);
        List<Event> owned = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (events != null)
            for (Event event : events)
                if (event != null && event.id != null && event.status != null && event.propertyId == id && ids.contains(event.deviceId) && seen.add(event.id)) owned.add(event);
        return new LeakMonitoringData(id, name, active, owned, active.isEmpty() ? Availability.NO_METER : Availability.AVAILABLE);
    }

    public static List<ApiModels.DeviceResponse> activeDevices(Integer propertyId, List<ApiModels.DeviceResponse> values) {
        List<ApiModels.DeviceResponse> active = new ArrayList<>();
        if (propertyId != null && values != null)
            for (ApiModels.DeviceResponse device : values)
                if (device != null && propertyId.equals(device.propertyId) && Boolean.TRUE.equals(device.isActive)) active.add(device);
        return active;
    }

    public static List<Sample> orderedSamples(List<Sample> values) {
        SortedMap<Long, Sample> ordered = new TreeMap<>();
        if (values != null)
            for (Sample sample : values) if (sample != null) ordered.put(sample.timestamp, sample);
        return new ArrayList<>(ordered.values());
    }

    private static BigDecimal validFlow(BigDecimal value) {
        return value != null && value.signum() >= 0 && Double.isFinite(value.doubleValue()) ? value : null;
    }
}
