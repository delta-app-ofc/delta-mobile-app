package com.aula.delta_mobile_app.model;

import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AuthModels {
    private AuthModels() {}

    public static class Credentials {
        public String email, password;

        public Credentials(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class CreateAccount extends Credentials {
        public String name, birthDate, phone;

        public CreateAccount(String name, String birthDate, String phone, String email, String password) {
            super(email, password);
            this.name = name;
            this.birthDate = birthDate;
            this.phone = phone;
        }
    }

    public static class Tokens {
        public String accessToken, tokenType, refreshToken;
        public long expiresIn, refreshExpiresIn;
    }

    public static class Refresh {
        public final String refreshToken;

        public Refresh(String token) {
            refreshToken = token;
        }
    }

    public static class Profile {
        public Integer id;
        public String name, email;
    }

    public static class PropertyLink {
        public Integer propertyId;
    }

    public static class Address {
        public Integer regionId;
        public String cep, city, state;
        public BigDecimal latitude, longitude;
    }

    public static class Property {
        public String name, type, classification;
        public Integer organizationId;
        public BigDecimal builtAreaM2;
    }

    public static class Habit {
        public Integer habitId, frequency;
        public List<Integer> daysOfWeek = new ArrayList<>();
    }

    public static class Completion {
        public Address address;
        public Property property;
        public List<Habit> habits = new ArrayList<>();
    }

    public static class Completed {
        public Integer userId;
        public boolean completed;

        public transient boolean recoveredExisting;

        public JsonObject address, property;
        public List<JsonObject> habits;
    }
}
