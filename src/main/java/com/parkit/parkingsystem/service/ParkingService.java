package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Date;

public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private InputReaderUtil inputReaderUtil;
    private ParkingSpotDAO parkingSpotDAO;
    private TicketDAO ticketDAO;
    private FareCalculatorService fareCalculatorService;

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO,
            TicketDAO ticketDAO, FareCalculatorService fareCalculatorService) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
        this.fareCalculatorService = fareCalculatorService;
    }

    public void processIncomingVehicle() throws Exception {
        try {
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if (parkingSpot != null && parkingSpot.getId() > 0) {
                String vehicleRegNumber = getVehichleRegNumber();
                boolean discount = false;
                if (ticketDAO.getNbTicket(vehicleRegNumber) > 0) {
                    discount = true;
                }
                parkingSpot.setAvailable(false);
                parkingSpotDAO.updateParking(parkingSpot);
                Date inTime = new Date();
                Ticket ticket = new Ticket();
                // ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
                // ticket.setId(ticketID);
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(0);
                ticket.setInTime(inTime);
                ticket.setOutTime(null);
                ticket.setDiscount(discount);
                if (ticketDAO.saveTicket(ticket)) {
                    System.out.println("Generated Ticket and saved in DB");
                    if (discount) {
                        System.out.println(
                                "Nice to see you again! As a recurrent user, you'll get a 5% discount on your parking fare");
                    }
                    System.out.println(
                            "Please park your vehicle in spot number: " + parkingSpot.getId());
                    System.out.println(
                            "For vehicle number:" + vehicleRegNumber + ", in-time is: " + inTime);
                } else {
                    throw new SQLException("Unable to save ticket informations. Error occurred");
                }
            }
        } catch (SQLException e) {
            logger.error("Unable to save ticket informations. Error occured", e);
            throw new SQLException();
        } catch (Exception e) {
            logger.error("Unable to process incoming vehicle", e);
            throw new Exception();
        }
    }

    private String getVehichleRegNumber() {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    public ParkingSpot getNextParkingNumberIfAvailable() throws Exception {
        int parkingNumber = 0;
        ParkingSpot parkingSpot = null;
        try {
            ParkingType parkingType = getVehichleType();
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if (parkingNumber > 0) {
                parkingSpot = new ParkingSpot(parkingNumber, parkingType, true);
            } else {
                throw new Exception(
                        "Error fetching parking number from DB. Parking slots might be full");
            }
        } catch (IllegalArgumentException ie) {
            logger.error("Error parsing user input for type of vehicle", ie);
        }
        return parkingSpot;
    }

    private ParkingType getVehichleType() {
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch (input) {
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    public void processExitingVehicle() throws Exception {
        String vehicleRegNumber = getVehichleRegNumber();
        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
        if (ticket == null) {
            System.out.println("No ticket was found with this registration number");
            return;
        }
        Date outTime = new Date();
        ticket.setOutTime(outTime);
        fareCalculatorService.calculateFare(ticket);
        if (ticketDAO.updateTicket(ticket)) {
            ParkingSpot parkingSpot = ticket.getParkingSpot();
            parkingSpot.setAvailable(true);
            parkingSpotDAO.updateParking(parkingSpot);
            System.out.println("Please pay the parking fare: " + ticket.getPrice());
            System.out.println("Recorded out-time for vehicle number: "
                    + ticket.getVehicleRegNumber() + " is: " + outTime);
        } else {
            logger.error("Unable to process exiting vehicle");
            throw new SQLException("Unable to update ticket information. Error occurred");
        }
    }
}
