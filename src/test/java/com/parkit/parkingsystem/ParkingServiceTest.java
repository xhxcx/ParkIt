package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingCustomerDAO;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.model.ParkingCustomer;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;
    private static Ticket ticket = new Ticket();

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @Mock
    private static ParkingCustomerDAO parkingCustomerDAO;


    @BeforeEach
    private void setUpPerTest() {
        try {
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            ticket.setIsEligibleForRecurringUser(false);
            parkingSpotDAO.dataBaseConfig=new DataBaseTestConfig();
            ticketDAO.dataBaseConfig = new DataBaseTestConfig();
            parkingCustomerDAO.dataBaseConfig = new DataBaseTestConfig();

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, parkingCustomerDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processIncomingNonRecurringCarVehicleTest(){
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        parkingService.processIncomingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void createParkingCustomerTest(){
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ZZZZZ");
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        parkingService.processIncomingVehicle();
        verify(parkingCustomerDAO, Mockito.times(1)).saveParkingCustomer(any(ParkingCustomer.class));
    }

    @Test
    public void processIncomingForAParkingCustomerTest(){
        ParkingCustomer customer = new ParkingCustomer();
        customer.setId(1);
        customer.setPlateNumber("ABCDEF");
        customer.setDiscountBonus(Fare.RECURRING_USER_DISCOUNT);
        when(parkingCustomerDAO.getParkingCustomer(any(String.class))).thenReturn(customer);

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        parkingService.processIncomingVehicle();

        verify(parkingCustomerDAO, Mockito.times(1)).getParkingCustomer(any(String.class));
        verify(parkingCustomerDAO, Mockito.times(0)).saveParkingCustomer(any(ParkingCustomer.class));
    }

    @Nested
    @DisplayName("Tests for non passing case")
    class processingWithErrors{

        @Test
        public void getNextAvailableParkingSpot_shouldReturnNull_whenParkingSpotIsInvalid(){
            when(inputReaderUtil.readSelection()).thenReturn(1);
            when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(-1);
            assertNull(parkingService.getNextParkingNumberIfAvailable());
        }

        @Test
        public void getNextAvailableParkingSpot_shouldReturnNull_whenParkingTypeIsInvalid(){
            when(inputReaderUtil.readSelection()).thenReturn(-1);
            assertNull(parkingService.getNextParkingNumberIfAvailable());
        }

        @Test
        public void processExitingVehicle_shouldFailWithNoParkingUpdate_whenTicketCantBeRetrieved(){
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            when(ticketDAO.getTicket(anyString())).thenReturn(null);
            parkingService.processExitingVehicle();
            verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
        }

        @Test
        public void processExitingVehicle_shouldFailWithNoParkingUpdate_whenTicketUpdateFailed(){
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(ticket)).thenReturn(false);
            parkingService.processExitingVehicle();
            verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
        }
    }

}
