package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private static DataBasePrepareService dataBasePrepareService;
    private static FareCalculatorService fareCalculatorService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @Spy
    private static TicketDAO ticketDAO;

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
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {
    }

    @Test
    public void testParkingACar() throws Exception {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
                fareCalculatorService);

        parkingService.processIncomingVehicle();

        Connection con = dataBaseTestConfig.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT AVAILABLE FROM parking WHERE PARKING_NUMBER = 1");
        ResultSet rs = ps.executeQuery();
        boolean isAvailable = true;
        if (rs.next()) {
            isAvailable = rs.getBoolean("AVAILABLE");
        }
        dataBaseTestConfig.closeResultSet(rs);
        dataBaseTestConfig.closePreparedStatement(ps);
        dataBaseTestConfig.closeConnection(con);

        assertFalse(isAvailable);
        verify(inputReaderUtil, Mockito.times(1)).readSelection();
        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
    }

    @Test
    public void testParkingLotExit() throws Exception {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
                fareCalculatorService);
        parkingService.processIncomingVehicle();

        parkingService.processExitingVehicle();

        Connection con = dataBaseTestConfig.getConnection();
        PreparedStatement ps = con
                .prepareStatement("SELECT OUT_TIME, PRICE FROM ticket WHERE `VEHICLE_REG_NUMBER` = ?");
        ps.setString(1, "ABCDEF");
        ResultSet rs = ps.executeQuery();
        Date outTime = null;
        Object price = null;
        if (rs.next()) {
            outTime = rs.getTimestamp("OUT_TIME");
            price = rs.getDouble("PRICE");
        }
        dataBaseTestConfig.closeResultSet(rs);
        dataBaseTestConfig.closePreparedStatement(ps);
        dataBaseTestConfig.closeConnection(con);

        assertNotNull(outTime);
        assertNotNull(price);
        verify(inputReaderUtil, Mockito.times(1)).readSelection();
        verify(inputReaderUtil, Mockito.times(2)).readVehicleRegistrationNumber();
    }

    @Test
    public void ParkingLotExitRecurringUserTest() throws Exception {
        // Arrange
        when(ticketDAO.getNbTicket(any(String.class))).thenReturn(2);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
                fareCalculatorService);
        parkingService.processIncomingVehicle();

        Connection con2 = dataBaseTestConfig.getConnection();
        PreparedStatement ps2 = con2.prepareStatement("UPDATE ticket SET IN_TIME = ? WHERE ID = ?");
        ps2.setTimestamp(1, Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS)));
        ps2.setInt(2, 1);
        ps2.execute();
        dataBaseTestConfig.closePreparedStatement(ps2);
        dataBaseTestConfig.closeConnection(con2);

        // Act
        parkingService.processExitingVehicle();

        // Assert
        Connection con3 = dataBaseTestConfig.getConnection();
        PreparedStatement ps3 = con3.prepareStatement("SELECT PRICE FROM ticket WHERE ID = ?");
        ps3.setInt(1, 1);
        ResultSet rs = ps3.executeQuery();
        double calculatedPrice = 0;
        if (rs.next()) {
            calculatedPrice = rs.getDouble("PRICE");
        }
        dataBaseTestConfig.closeResultSet(rs);
        dataBaseTestConfig.closePreparedStatement(ps3);
        dataBaseTestConfig.closeConnection(con3);

        double expectedPrice = 1.5 * 2*0.95;
        double delta = 0.001;

        assertEquals(expectedPrice, calculatedPrice, delta);
        verify(ticketDAO, Mockito.times(2)).getNbTicket(any(String.class));

    }

}
