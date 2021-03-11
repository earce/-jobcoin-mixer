package com.gemini.jobcoin.helper;

import com.gemini.jobcoin.exception.JobcoinException;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

public class JobcoinMathTest {

    @Test
    public void zeroAndNegative() {

        boolean threw = false;

        try {
            JobcoinMath.breakUpDecimalIntoDecimals("0", 3);
        } catch (JobcoinException e) {
            threw = true;
        }

        Assert.assertTrue(threw);

        boolean threw1 = false;

        try {
            JobcoinMath.breakUpDecimalIntoDecimals("-0.01", 3);
        } catch (JobcoinException e) {
            threw1 = true;
        }

        Assert.assertTrue(threw1);
    }


    @Test
    public void undividable() throws JobcoinException {
        List<BigDecimal> l1 = JobcoinMath.breakUpDecimalIntoDecimals("0.01", 5);
        Assert.assertEquals(l1.size(), 1);
        Assert.assertEquals(l1.get(0), new BigDecimal(".01"));

        List<BigDecimal> l2 = JobcoinMath.breakUpDecimalIntoDecimals("0.0001", 5);
        Assert.assertEquals(l2.size(), 1);
        Assert.assertEquals(l2.get(0), new BigDecimal(".0001"));
    }


    @Test
    public void testing100Random() throws JobcoinException {
        boolean someLargerThenOne = false;
        for (int i = 0; i < 100; i++) {
            List<BigDecimal> l1 = JobcoinMath.breakUpDecimalIntoDecimals("100", new SecureRandom().nextInt(5));
            Assert.assertTrue(l1.size() >= 1 && l1.size() <= 5);
            if (l1.size() > 1) {
                someLargerThenOne = true;
            }
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal bd : l1) {
                total = total.add(bd);
            }
            Assert.assertEquals(new BigDecimal("100").compareTo(total), 0);
        }
        Assert.assertTrue(someLargerThenOne);
    }

    @Test
    public void testingRandomDecimal() throws JobcoinException {
        boolean someLargerThenOne = false;
        for (int i = 0; i < 100; i++) {
            List<BigDecimal> l1 = JobcoinMath.breakUpDecimalIntoDecimals("125.014", new SecureRandom().nextInt(5));
            Assert.assertTrue(l1.size() >= 1 && l1.size() <= 5);
            if (l1.size() > 1) {
                someLargerThenOne = true;
            }
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal bd : l1) {
                total = total.add(bd);
            }
            Assert.assertEquals(new BigDecimal("125.014").compareTo(total), 0);
        }
        Assert.assertTrue(someLargerThenOne);

        boolean someLargerThenOne1 = false;
        for (int i = 0; i < 100; i++) {
            List<BigDecimal> l1 = JobcoinMath.breakUpDecimalIntoDecimals("0.014", new SecureRandom().nextInt(5));
            Assert.assertTrue(l1.size() >= 1 && l1.size() <= 5);
            if (l1.size() > 1) {
                someLargerThenOne1 = true;
            }
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal bd : l1) {
                total = total.add(bd);
            }
            Assert.assertEquals(new BigDecimal("0.014").compareTo(total), 0);
        }
        Assert.assertTrue(someLargerThenOne1);
    }
}
