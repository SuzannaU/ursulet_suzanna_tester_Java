package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;
    private Date inTime;
    private Date outTime;
    private ParkingSpot parkingSpot;

    @BeforeAll
    static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    void setUpPerTest() {
        ticket = new Ticket();
        outTime = new Date();
    }

    private void initializeTicket(ParkingType type, long durationInMs, boolean discount) {
        inTime = new Date(System.currentTimeMillis() - durationInMs);
        parkingSpot = new ParkingSpot(1, type, false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setDiscount(discount);
    }

    @Test
    void calculateFare_withCorrectCarParameters_setsTheTicketPrice() {
        initializeTicket(ParkingType.CAR, 60 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_withCorrectBikeParameters_setsTheTicketPrice() {
        initializeTicket(ParkingType.BIKE, 60 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_withUnknownType_throwsException() {
        initializeTicket(null, 60 * 60 * 1000, false);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    void calculateFare_whenCarInTimeInFuture_throwsException() {
        inTime = new Date(System.currentTimeMillis() + (60 * 60 * 1000));
        parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setDiscount(false);
        assertThrows(
                IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    void calculateFare_whenBikeInTimeInFuture_throwsException() {
        inTime = new Date(System.currentTimeMillis() + (60 * 60 * 1000));
        parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setDiscount(false);
        assertThrows(
                IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    void calculateFare_whenCarParkingTimeUnderOneHour_setsTicketPrice() {
        initializeTicket(ParkingType.CAR, 45 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.75 * Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_whenBikeParkingTimeUnderOneHour_setsTicketPrice() {
        initializeTicket(ParkingType.BIKE, 45 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.75 * Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_whenCarParkingTimeUnderThirtyMinutes_setsTicketPriceToZero() {
        initializeTicket(ParkingType.CAR, 15 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_whenBikeParkingTimeUnderThirtyMinutes_setsTicketPriceToZero() {
        initializeTicket(ParkingType.BIKE, 15 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_whenCarParkingTimeOverADay_setsTicketPrice() {
        initializeTicket(ParkingType.CAR, 24 * 60 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(24 * Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_whenBikeParkingTimeOverADay_setsTicketPrice() {
        initializeTicket(ParkingType.BIKE, 24 * 60 * 60 * 1000, false);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(24 * Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_whenCarTicketDiscountIsTrue_setsTicketPriceWithDiscount() {
        initializeTicket(ParkingType.CAR, 60 * 60 * 1000, true);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.95 * Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    void calculateFare_whenBikeTicketDiscountIsTrue_setsTicketPriceWithDiscount() {
        initializeTicket(ParkingType.BIKE, 60 * 60 * 1000, true);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.95 * Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }
}
