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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
                    fareCalculatorService);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processIncomingVehicle_withCorrectParameters_doesNotThrow() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        assertDoesNotThrow(() -> {
            parkingService.processIncomingVehicle();
        });

        verify(inputReaderUtil, Mockito.times(1)).readSelection();
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, Mockito.times(1)).getNbTicket(any(String.class));
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
    }

    @Nested
    @Tag("processExitingVehicle tests")
    class processExitingVehicleTests {
        @BeforeEach
        private void setUpExiting() {
            try {
                ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
                Ticket ticket = new Ticket();
                ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
                ticket.setOutTime(new Date(System.currentTimeMillis()));
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber("ABCDEF");
                when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
                when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to set up test mock objects");
            }
        }

        @Test
        public void processExitingVehicle_withCorrectParameters_doesNotThrow() {
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            assertDoesNotThrow(() -> {
                parkingService.processExitingVehicle();
            });

            verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
            verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
            verify(fareCalculatorService, Mockito.times(1)).calculateFare(any(Ticket.class));
            verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
            verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        }

        @Test
        public void processExitingVehicle_whenUnableToUpdateDB_throwsException() {
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

            assertThrows(SQLException.class, () -> parkingService.processExitingVehicle());

            verify(ticketDAO, Mockito.times(1)).getTicket(any(String.class));
            verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
            verify(fareCalculatorService, Mockito.times(1)).calculateFare(any(Ticket.class));
            verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        }
    }

    @Nested
    @Tag("getNextParkingNumberIfAvailable tests")
    class getNextParkingNumberIfAvailableTests {
        @Test
        public void getNextParkingNumberIfAvailable_withCorrectParameters_returnsParkingSpot()
                throws Exception {
            when(inputReaderUtil.readSelection()).thenReturn(1);
            when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
            ParkingSpot expectedParkingSpot = new ParkingSpot(1, ParkingType.CAR, true);

            ParkingSpot testParkingSpot = parkingService.getNextParkingNumberIfAvailable();

            assertEquals(expectedParkingSpot, testParkingSpot);
            verify(inputReaderUtil, Mockito.times(1)).readSelection();
            verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(ParkingType.CAR);
        }

        @Test
        public void getNextParkingNumberIfAvailable_whenParkingNotFound_throwsException() {
            when(inputReaderUtil.readSelection()).thenReturn(1);
            when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

            assertThrows(Exception.class, () -> parkingService.getNextParkingNumberIfAvailable());

            verify(inputReaderUtil, Mockito.times(1)).readSelection();
            verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        }

        @Test
        public void getNextParkingNumberIfAvailable_whenParkingWrongType_returnsNullParkingSpot()
                throws Exception {
            when(inputReaderUtil.readSelection()).thenReturn(3);

            assertNull(parkingService.getNextParkingNumberIfAvailable());
            verify(inputReaderUtil, Mockito.times(1)).readSelection();
        }
    }
}
