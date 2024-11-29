package com.parkit.parkingsystem;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.InteractiveShell;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InteractiveShellTest {
    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @Mock
    private static FareCalculatorService fareCalculatorService;
    @Mock
    private static ParkingService parkingService;

    private static InteractiveShell interactiveShell;

    @BeforeEach
    private void setUpPerTest() {
        try {
            interactiveShell = new InteractiveShell(inputReaderUtil, parkingService);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void loadInterface_whenCase1_launchesProcessIncomingVehicle() throws Exception {
        when(inputReaderUtil.readSelection())
                .thenReturn(1)
                .thenReturn(3);
        doThrow(new Exception()).when(parkingService).processIncomingVehicle();

        assertThrows(Exception.class, () -> interactiveShell.loadInterface());

        verify(inputReaderUtil).readSelection();
        verify(parkingService).processIncomingVehicle();
    }

    @Test
    public void loadInterface_whenCase2_launchesProcessExitingVehicle() throws Exception {
        when(inputReaderUtil.readSelection())
                .thenReturn(2)
                .thenReturn(3);
        doThrow(new Exception()).when(parkingService).processExitingVehicle();

        assertThrows(Exception.class, () -> interactiveShell.loadInterface());

        verify(inputReaderUtil).readSelection();
        verify(parkingService).processExitingVehicle();
    }

    @Test
    public void loadInterface_whenCase3_printsMessage() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(3);
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        interactiveShell.loadInterface();

        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Exiting from the system!"));
        System.setOut(System.out);
    }

    @Test
    public void loadInterface_whenDefault_printsMessage() throws Exception {
        when(inputReaderUtil.readSelection())
                .thenReturn(4)
                .thenReturn(3);
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        interactiveShell.loadInterface();

        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains(
                "Unsupported option. Please enter a number corresponding to the provided menu"));
        System.setOut(System.out);
    }
}
