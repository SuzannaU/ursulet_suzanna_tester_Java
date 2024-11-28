package com.parkit.parkingsystem.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class InputReaderUtil {

    private static Scanner scan = new Scanner(System.in, StandardCharsets.UTF_8); // Specifies UTF-8 to avoid cross plateform issues
    private static final Logger logger = LogManager.getLogger("InputReaderUtil");

    public int readSelection() {
        try {
            int input = Integer.parseInt(scan.nextLine());
            return input;
        } catch (NumberFormatException e) {
            logger.error("Error while reading user input from Shell", e);
            System.out.println(
                    "Error reading input. Please enter valid number for proceeding further");
            return -1;
        }
    }

    public String readVehicleRegistrationNumber() {
        String vehicleRegNumber = scan.nextLine();
        if (vehicleRegNumber == null || vehicleRegNumber.trim().length() == 0) {
            logger.error("Error while reading user input from Shell");
            System.out.println(
                    "Error reading input. Please enter a valid string for vehicle registration number");
            throw new IllegalArgumentException("Invalid input provided");
        }
        return vehicleRegNumber;
    }
}
