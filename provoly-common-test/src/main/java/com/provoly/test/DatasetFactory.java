package com.provoly.test;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DatasetFactory {
    public static final UUID BIKE_STATION_DATASOURCE_ID = UUID.fromString("423b5c01-1816-41d4-b358-100000000000");
    public static final int BIKE_STATION_SIZE = 3;
    public static final int BIKE_STATION_NB_ATTRIBUTES = 3;
    public static final UUID BIKE_STATION_OCLASS_ID = UUID.fromString("423b5c01-1816-41d4-b358-100000000001");

    public static final UUID BIKE_STATION_DATASET = UUID.fromString("423b5c01-1816-41d4-b358-100000000002");

    public List<BikeStation> getBikeStations() {
        return List.of(
                new BikeStation(
                        "Marché des Chartrons",
                        23,
                        12),
                new BikeStation(
                        "Parlement",
                        5,
                        0),
                new BikeStation(
                        "Fernand Lafargue",
                        12,
                        11));
    }
}
