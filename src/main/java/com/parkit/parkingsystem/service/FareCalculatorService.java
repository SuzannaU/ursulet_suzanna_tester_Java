package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare (Ticket ticket){
        calculateFare (ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        double inHour = ticket.getInTime().getTime(); // changed getHour to getTime - changed type from int to double
        double outHour = ticket.getOutTime().getTime();
        double duration = (outHour - inHour) / 3_600_000; // converts milliseconds to hours - changed type from int to double
        //boolean discount = ticket.getDiscount();
        if (duration < 0.5) {
            ticket.setPrice(0);
        } else {
            double basePrice;
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR -> basePrice = duration * Fare.CAR_RATE_PER_HOUR;
                case BIKE -> basePrice = duration * Fare.BIKE_RATE_PER_HOUR;
                // case CAR -> ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                // case BIKE -> ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                default -> throw new IllegalArgumentException("Unkown Parking Type");
            }
            if (discount) {
                basePrice *= 0.95;
            }
            ticket.setPrice(basePrice);
        }
    }
}