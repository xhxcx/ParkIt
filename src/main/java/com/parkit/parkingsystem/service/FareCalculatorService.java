package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.time.Duration;

/**
 * Fare calculator to set price for a given Ticket
 *
 */
public class FareCalculatorService {

    /**
     * Calculate a fee based on a Ticket
     * get entry and exit hours
     * then multiply the duration of stay by the rate regarding the parking type
     *
     * @param ticket
     */
    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        //Get entry and exit hours in millis
        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        //Calculate the duration and convert it from millis to hours by dividing by 3600*1000
        double duration = (double)Duration.ofMillis(outHour - inHour).toMillis()/3600000;

        //Apply fare regarding parking type
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}