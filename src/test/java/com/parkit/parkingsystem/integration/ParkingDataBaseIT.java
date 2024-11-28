package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig;
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static FareCalculatorService fareCalculatorService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    private ParkingService parkingService;

    @BeforeAll
    private static void setUp() throws Exception {
        dataBaseTestConfig = new DataBaseTestConfig();
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(
                inputReaderUtil,
                parkingSpotDAO,
                ticketDAO,
                fareCalculatorService);
    }

    @AfterAll
    private static void tearDown() {}

    @Test
    public void processIncommingVehicle_withCorrectParamters_setsParkingSpotUnavailable()
            throws Exception {
        parkingService.processIncomingVehicle();

        String statement = "SELECT AVAILABLE FROM parking WHERE PARKING_NUMBER = 1";
        try (Connection con = dataBaseTestConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(statement);) {
            try (ResultSet rs = ps.executeQuery();) {
                boolean isAvailable = true;
                if (rs.next()) {
                    isAvailable = rs.getBoolean("AVAILABLE");
                }
                assertFalse(isAvailable);
            }
        }

        verify(inputReaderUtil).readSelection();
        verify(inputReaderUtil).readVehicleRegistrationNumber();
    }

    @Test
    public void processExitingVehicle_withCorrectParameters_setsOutTimeAndPrice() throws Exception {
        parkingService.processIncomingVehicle();
        Thread.sleep(1000); // adds delay to ensure OUT_TIME is always after IN_TIME

        parkingService.processExitingVehicle();
        String statement = "SELECT OUT_TIME, PRICE FROM ticket WHERE `VEHICLE_REG_NUMBER` = ?";
        try (Connection con = dataBaseTestConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(statement);) {
            ps.setString(1, "ABCDEF");
            try (ResultSet rs = ps.executeQuery();) {
                Date outTime = null;
                Object price = null;
                if (rs.next()) {
                    outTime = rs.getTimestamp("OUT_TIME");
                    price = rs.getDouble("PRICE");
                }
                assertNotNull(outTime);
                assertNotNull(price);
            }
        }

        verify(inputReaderUtil).readSelection();
        verify(inputReaderUtil, Mockito.times(2)).readVehicleRegistrationNumber();
    }

    @Test
    public void processExitingVehicle_forRecurringUSer_setsDiscountedPrice() throws Exception {
        // Insert initial ticket for recurring user
        String initialStatement =
                "INSERT INTO TICKET (PARKING_NUMBER, VEHICLE_REG_NUMBER, IN_TIME, OUT_TIME,PRICE) values(?,?,?,?,?)";
        try (Connection con = dataBaseTestConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(initialStatement);) {
            ps.setInt(1, 1);
            ps.setString(2, "ABCDEF");
            ps.setTimestamp(3, Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
            ps.setTimestamp(4, Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
            ps.setDouble(5, 1.0);
            ps.execute();
        }

        parkingService.processIncomingVehicle();

        // Update test ticket for a 2-hour Stay
        String alterStatement = "UPDATE ticket SET IN_TIME = ? WHERE ID = ?";
        try (Connection con = dataBaseTestConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(alterStatement);) {
            ps.setTimestamp(1, Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS)));
            ps.setInt(2, 2);
            ps.execute();
        }

        // Act
        parkingService.processExitingVehicle();

        // Assert
        String resultStatement = "SELECT PRICE FROM ticket WHERE ID = ?";
        try (Connection con = dataBaseTestConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(resultStatement);) {
            ps.setInt(1, 2);
            try (ResultSet rs = ps.executeQuery();) {
                double calculatedPrice = 0;
                if (rs.next()) {
                    calculatedPrice = rs.getDouble("PRICE");
                }
                double expectedPrice = 1.5 * 2 * 0.95;
                assertEquals(expectedPrice, calculatedPrice, 0.001);
            }
        }
        verify(inputReaderUtil).readSelection();
        verify(inputReaderUtil, Mockito.times(2)).readVehicleRegistrationNumber();
    }
}
