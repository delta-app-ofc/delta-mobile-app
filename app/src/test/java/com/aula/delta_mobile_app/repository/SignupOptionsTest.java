package com.aula.delta_mobile_app.repository;

import static org.junit.Assert.*;

import org.junit.Test;

public class SignupOptionsTest {
    @Test
    public void missingMappingsNeverFallBackToExampleIds() {
        SignupOptions catalog = new SignupOptions();
        assertNull(catalog.habit("shower"));
        assertNull(catalog.region("SP", "São Paulo"));
    }

    @Test
    public void regionsRequireExactConfiguredLocalityAndRejectAmbiguity() {
        SignupOptions catalog = new SignupOptions();
        SignupOptions.Region region = new SignupOptions.Region();
        region.state = "SP";
        region.city = "São Paulo";
        region.regionId = 42;
        catalog.regions.add(region);
        assertEquals(Integer.valueOf(42), catalog.region("sp", "Sao Paulo"));
        assertNull(catalog.region("SP", "Campinas"));
        SignupOptions.Region conflict = new SignupOptions.Region();
        conflict.state = "SP";
        conflict.city = "São Paulo";
        conflict.regionId = 99;
        catalog.regions.add(conflict);
        assertNull(catalog.region("SP", "São Paulo"));
    }

    @Test
    public void habitIdsComeFromKeysNotVisualOrder() {
        SignupOptions catalog = new SignupOptions();
        SignupOptions.Entry entry = new SignupOptions.Entry();
        entry.key = "shower";
        entry.id = 87;
        catalog.habits.add(entry);
        assertEquals(Integer.valueOf(87), catalog.habit("shower"));
    }
}
