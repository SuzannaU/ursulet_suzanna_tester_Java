package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException(
                    "Out time provided is incorrect:" + ticket.getOutTime().toString());
        }
        long inHour = (ticket.getInTime().getTime()) / 60_000;
        long outHour = (ticket.getOutTime().getTime()) / 60_000;
        long duration = (outHour - inHour);
        if (duration < 30) {
            ticket.setPrice(0.0);
        } else {
            double basePrice;
            switch (ticket.getParkingSpot().getParkingType()) {
                case CAR: {
                    basePrice = duration / 60.0 * Fare.CAR_RATE_PER_HOUR;
                    break;
                }
                case BIKE: {
                    basePrice = duration / 60.0 * Fare.BIKE_RATE_PER_HOUR;
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unkown Parking Type");
            }
            if (ticket.getDiscount()) {
                basePrice *= 0.95;
            }
            ticket.setPrice(basePrice);
        }
    }
}
