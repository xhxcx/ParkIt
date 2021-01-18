package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private static ParkingSpot carParkingSpot;
    private static ParkingSpot bikeParkingSpot;
    private Ticket ticket;
    private Date inTime;
    private Date outTime;

    @BeforeAll
    private static void setUp() {
        fareCalculatorService = new FareCalculatorService();
        carParkingSpot = new ParkingSpot(1,ParkingType.CAR,false);
        bikeParkingSpot = new ParkingSpot(1,ParkingType.BIKE,false);
    }

    @BeforeEach
    private void setUpPerTest() {
        inTime = new Date();
        outTime = new Date();
        ticket = new Ticket();
        ticket.setIsEligibleForRecurringUser(false);
    }

    @Test
    public void calculateFareCar(){
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(carParkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.CAR_RATE_PER_HOUR);
    }

    @Test
    public void calculateFareBike(){
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(bikeParkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), Fare.BIKE_RATE_PER_HOUR);
    }

    @Test
    public void calculateFareUnkownType(){
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        ParkingSpot parkingSpot = new ParkingSpot(1, null,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithFutureInTime(){
        inTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(bikeParkingSpot);
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(bikeParkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice() );
    }

    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );//45 minutes parking time should give 3/4th parking fare
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( (0.75 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithMoreThanADayParkingTime(){
        inTime.setTime( System.currentTimeMillis() - (  24 * 60 * 60 * 1000) );//24 hours parking time should give 24 * parking fare per hour

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(carParkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Nested
    @DisplayName("Check conditions to not apply the classic fare calculation")
    class DiscountTests {

        @Tag("Under30")
        @Test
        public void calculateFareBikeWithLessThanThirtyMinutesParkingTime(){
            inTime.setTime( System.currentTimeMillis() - (  15 * 60 * 1000) );//15 minutes parking time should be free

            ticket.setInTime(inTime);
            ticket.setOutTime(outTime);
            ticket.setParkingSpot(bikeParkingSpot);
            fareCalculatorService.calculateFare(ticket);
            assertEquals(0, ticket.getPrice() );
        }

        @Tag("Under30")
        @Test
        public void calculateFareCarWithLessThanThirtyMinutesParkingTime(){
            inTime.setTime( System.currentTimeMillis() - (  29 * 60 * 1000) );//29 minutes parking time should be free

            ticket.setInTime(inTime);
            ticket.setOutTime(outTime);
            ticket.setParkingSpot(carParkingSpot);
            fareCalculatorService.calculateFare(ticket);
            assertEquals( 0 , ticket.getPrice());
        }

        @Tag("CustomerDiscount")
        @Test
        public void calculateFareCarWithMoreThanThirtyMinutesParkingTimeAndRecurringDiscount() {
            inTime.setTime(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 24 hours parking time

            ticket.setInTime(inTime);
            ticket.setOutTime(outTime);
            ticket.setParkingSpot(carParkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            ticket.setIsEligibleForRecurringUser(true);

            fareCalculatorService.calculateFare(ticket);

            assertEquals(((24 * Fare.CAR_RATE_PER_HOUR) - (Fare.RECURRING_USER_DISCOUNT * (24 * Fare.CAR_RATE_PER_HOUR))), ticket.getPrice());
        }

        @Tag("CustomerDiscount")
        @Test
        public void calculateFareBikeWithMoreThanThirtyMinutesParkingTimeAndRecurringDiscount() {
            inTime.setTime(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // 24 hours parking time

            ticket.setInTime(inTime);
            ticket.setOutTime(outTime);
            ticket.setParkingSpot(bikeParkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            ticket.setIsEligibleForRecurringUser(true);

            fareCalculatorService.calculateFare(ticket);

            assertEquals(((24 * Fare.BIKE_RATE_PER_HOUR) - (Fare.RECURRING_USER_DISCOUNT * (24 * Fare.BIKE_RATE_PER_HOUR))), ticket.getPrice());
        }
    }

}
