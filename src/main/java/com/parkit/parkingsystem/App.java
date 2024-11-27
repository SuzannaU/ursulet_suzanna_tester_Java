package com.parkit.parkingsystem;

import com.parkit.parkingsystem.dao.*;
import com.parkit.parkingsystem.service.*;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {
    private static final Logger logger = LogManager.getLogger("App");

    public static void main(String args[]) throws Exception {
        logger.info("Initializing Parking System");
        InputReaderUtil inputReaderUtil = new InputReaderUtil();
        ParkingSpotDAO parkingSpotDAO = new ParkingSpotDAO();
        TicketDAO ticketDAO = new TicketDAO();
        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        ParkingService parkingService = new ParkingService(
                inputReaderUtil,
                parkingSpotDAO,
                ticketDAO,
                fareCalculatorService);
        InteractiveShell interactiveShell = new InteractiveShell(inputReaderUtil, parkingService);
        interactiveShell.loadInterface();
    }
}
