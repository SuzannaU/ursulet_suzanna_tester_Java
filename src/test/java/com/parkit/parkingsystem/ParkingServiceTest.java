package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @Mock
    private static FareCalculatorService fareCalculatorService;

    @BeforeEach
    private void setUpPerTest() {
        try {
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
            ticket.setOutTime(new Date(System.currentTimeMillis()));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            lenient().when(inputReaderUtil.readSelection()).thenReturn(1);

            lenient().when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(ticketDAO.getNbTicket(any(String.class))).thenReturn(1);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processIncomingVehicleTest() {
        parkingService.processIncomingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));

    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        parkingService.processExitingVehicle();
        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
        verify(fareCalculatorService, Mockito.times(1)).calculateFare(any(Ticket.class), anyBoolean());
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

    }

    @Test
    public void processExitingVehicleTest_UnableUpdate() throws Exception {
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        assertThrows(SQLException.class, () -> parkingService.processExitingVehicle());        
        
        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
        verify(fareCalculatorService, Mockito.times(1)).calculateFare(any(Ticket.class), anyBoolean());
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
    }

    @Test
    public void getNextParkingNumberIfAvailableTest() throws Exception {
        ParkingSpot expectedParkingSpot = new ParkingSpot(1, ParkingType.CAR, true);

        ParkingSpot testParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertEquals(expectedParkingSpot, testParkingSpot);
    }

    @Test
    public void getNextParkingNumberIfAvailable_parkingNotFound () {
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);
        
        assertThrows(Exception.class, () -> parkingService.getNextParkingNumberIfAvailable());
    }

    @Test
    public void getNextParkingNumberIfAvailableTest_parkingNumberWrongArgument() throws Exception{
        when(inputReaderUtil.readSelection()).thenReturn(3);

        assertNull(parkingService.getNextParkingNumberIfAvailable());
    }

}
