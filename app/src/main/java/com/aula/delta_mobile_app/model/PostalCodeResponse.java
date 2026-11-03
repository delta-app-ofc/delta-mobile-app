package com.aula.delta_mobile_app.model;

public class PostalCodeResponse {
    public String state;
    public String city;
    public Location location;

    public static class Location {
        public Coordinates coordinates;
    }

    public static class Coordinates {
        public Double latitude;
        public Double longitude;
    }
}
