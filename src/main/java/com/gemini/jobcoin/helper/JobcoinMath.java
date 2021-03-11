package com.gemini.jobcoin.helper;

import com.gemini.jobcoin.exception.JobcoinException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class JobcoinMath {

    private static final BigDecimal smallestDividable = new BigDecimal(".01");

    /**
     * Method takes a given amount and splits it up into the requested amount of
     * numbers partCount.
     *
     * When a random number is elected, if the number elected is greater then what would
     * be remaining, then what would be remaining is what gets added to the parts list
     * and the number elected gets used as the remaining. This is to remove the smallest
     * amount from the total each time.
     *
     * BigDecimal with a scale of 2 is used to protect against floating point precision
     * errors. If we get to a remaining of 0.01 or a number smaller then 0.01 was entered,
     * we break out and add that value as the final quantity. There are more performant
     * way to accomplish this but for simplicity using BigDecimal.
     *
     * This could be expanded to a smaller scale or adding a rule to enforce a minimum
     * transfer amount that is far greater then the smallest divisible unit we handle.
     *
     * Sample steps:

     * t1 = remaining -> 100.0, d -> 37.5, newRemaining = 62.5
     * t2 = remaining -> 62.5,  d -> 60.0, newRemaining = 2.5
     * t3 = remaining -> 60.0,  d -> 39.5, newRemaining = 20.5
     * t4 = remaining -> 39.5,  d -> 9.5,  newRemaining = 30.0
     * t5 = remaining -> 30.0
     *
     * quantities = [37.5, 2.5, 20.5, 9.5, 30]
     *
     * @param amount of to break up
     * @param partCount number of doubles to break it up into
     * @return a list of all the parts it was broken up into
     */
    public static LinkedList<BigDecimal> breakUpDecimalIntoDecimals(final String amount, int partCount)
            throws JobcoinException {

        BigDecimal remaining = new BigDecimal(amount);

        if (new BigDecimal(amount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new JobcoinException("Amount specified cannot be zero or below zero", 422);
        }

        final LinkedList<BigDecimal> quantities = new LinkedList<>();

        for (int i = 0; i < partCount - 1; i++) {
            if (remaining.compareTo(smallestDividable) <= 0) {  // edge case if we have not hit desired part
                break;                                          // count but we are at the maximum dividable
            }

            final double d = ThreadLocalRandom.current().nextDouble(0.0, remaining.doubleValue());
            final BigDecimal bd = new BigDecimal(d).setScale(2, RoundingMode.CEILING);

            final BigDecimal newRemaining = remaining.subtract(bd);

            if (bd.compareTo(newRemaining) < 0) { // always choose smaller to add
                quantities.add(bd);
                remaining = newRemaining;
            } else {
                quantities.add(newRemaining);
                remaining = bd;
            }
        }
        quantities.add(remaining);
        return quantities;
    }
}
