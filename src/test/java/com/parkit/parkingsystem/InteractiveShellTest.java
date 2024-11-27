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
        doThrow(new IllegalArgumentException()).when(parkingService).processIncomingVehicle();
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        assertThrows(IllegalArgumentException.class, () -> interactiveShell.loadInterface());
        interactiveShell.loadInterface();

        // Assert : Vérifier que le message "Exiting from the system!" est affiché
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains(
                "Please select vehicle type from menu"));

        verify(inputReaderUtil, times(2)).readSelection();
        verify(parkingService).processIncomingVehicle();
    }

    @Test
    public void loadInterface_whenCase2_launchesProcessExitingVehicle() throws Exception {
        when(inputReaderUtil.readSelection())
                .thenReturn(2)
                .thenReturn(3);
        doThrow(new IllegalArgumentException()).when(parkingService).processExitingVehicle();
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        assertThrows(IllegalArgumentException.class, () -> interactiveShell.loadInterface());
        interactiveShell.loadInterface();

        // Assert : Vérifier que le message "Exiting from the system!" est affiché
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains(
                "Please type the vehicle registration number and press enter key"));

        verify(inputReaderUtil, times(2)).readSelection();
        verify(parkingService).processExitingVehicle();
    }

    @Test
    public void loadInterface_whenCase3_printsMessage() throws Exception {
        // Arrange : Simule que l'utilisateur sélectionne l'option 3
        when(inputReaderUtil.readSelection()).thenReturn(3);

        // Redirect System.out to capture printed output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        // Act : Lancer la méthode
        interactiveShell.loadInterface();

        // Assert : Vérifier que le message "Exiting from the system!" est affiché
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains("Exiting from the system!"));

        // Clean-up : Reset System.out
        System.setOut(System.out);
    }

    @Test
    public void loadInterface_whenDefault_printsMessage() throws Exception {
        // Arrange : Simule que l'utilisateur sélectionne l'option 3
        when(inputReaderUtil.readSelection())
                .thenReturn(4)
                .thenReturn(3);

        // Redirect System.out to capture printed output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        // Act : Lancer la méthode
        interactiveShell.loadInterface();

        // Assert : Vérifier que le message "Exiting from the system!" est affiché
        String consoleOutput = outContent.toString();
        assertTrue(consoleOutput.contains(
                "Unsupported option. Please enter a number corresponding to the provided menu"));

        // Clean-up : Reset System.out
        System.setOut(System.out);
    }
}
