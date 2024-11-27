package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException(
                    "Out time provided is incorrect:" + ticket.getOutTime().toString());
        }
        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();
        double duration = (outHour - inHour) / 60_000;
        if (duration <= 29.9) {
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
