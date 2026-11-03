package com.aula.delta_mobile_app.repository;

import static org.junit.Assert.*;

import org.junit.Test;

public class SignupOptionsTest {
    @Test
    public void missingMappingsNeverFallBackToExampleIds() {
        SignupOptions catalog = new SignupOptions();
        assertNull(catalog.habit("shower"));
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
