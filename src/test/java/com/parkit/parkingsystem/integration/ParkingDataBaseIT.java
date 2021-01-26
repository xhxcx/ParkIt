package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingCustomerDAO;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingCustomer;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO = new ParkingSpotDAO();
    private static TicketDAO ticketDAO = new TicketDAO();
    private static ParkingCustomerDAO customerDAO = new ParkingCustomerDAO();
    private static DataBasePrepareService dataBasePrepareService = new DataBasePrepareService();
    private static Ticket myTicket;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        customerDAO.dataBaseConfig = dataBaseTestConfig;

        myTicket = null;

    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar(){
        //GIVEN see before each SetUpPerTest
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);

        // WHEN
        parkingService.processIncomingVehicle();
        myTicket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());

        // THEN : check ticket in DB information and availability of parking spot
            assertThat(myTicket).isNotNull();
            assertThat(myTicket.getVehicleRegNumber()).isEqualTo("ABCDEF");
            assertThat(myTicket.getParkingSpot().hashCode()).isEqualTo(1);
            assertThat(myTicket.getPrice()).isEqualTo(0);

            assertThat(parkingSpotDAO.getNextAvailableSlot(myTicket.getParkingSpot().getParkingType())).isNotEqualTo(1);
            assertThat(myTicket.getParkingSpot().isAvailable()).isFalse();
    }

    @Test
    public void testParkingARecurringCar(){
        //GIVEN see before each SetUpPerTest
        ParkingCustomer customer = new ParkingCustomer();
        customer.setPlateNumber("ABCDEF");
        customer.setDiscountBonus(Fare.RECURRING_USER_DISCOUNT);
        customerDAO.saveParkingCustomer(customer);


        // WHEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);
        parkingService.processIncomingVehicle();
        myTicket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());


        // THEN : check ticket in DB is eligible to the discount and check if the customer is correctly get
        assertThat(myTicket.isEligibleForRecurringUser()).isTrue();
        assertThat(customerDAO.getParkingCustomer(myTicket.getVehicleRegNumber())).isNotNull();
        assertThat(customerDAO.getParkingCustomer(myTicket.getVehicleRegNumber()).getDiscountBonus()).isEqualTo(0.05);
    }

    @Test
    public void testIncrementationOfParkingSpot(){
        //GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);
        ParkingSpot firstParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //WHEN
        parkingService.processIncomingVehicle();
        ParkingSpot secondParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //THEN verify if the 2 parking spots are different so that the update parking with the 1st is ok
        assertFalse(firstParkingSpot.equals(secondParkingSpot));
    }

    @Test
    public void testParkingLotExit(){

        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, customerDAO);
        parkingService.processIncomingVehicle();
        myTicket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());

        //Wait 2sec to have an exit timestamp after the entry
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // WHEN : process exiting
        parkingService.processExitingVehicle();
        // get ticket from DB
        Ticket outTicket = ticketDAO.getTicket(inputReaderUtil.readVehicleRegistrationNumber());

        // THEN : check exit time and if parking spot has been freed
        assertThat(outTicket.getOutTime()).isAfter(outTicket.getInTime());
        assertThat(parkingService.getNextParkingNumberIfAvailable().hashCode()).isEqualTo(1);

    }

}
