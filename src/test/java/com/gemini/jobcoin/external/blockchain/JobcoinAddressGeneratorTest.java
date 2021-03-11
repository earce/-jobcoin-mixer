package com.gemini.jobcoin.external.blockchain;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class JobcoinAddressGeneratorTest {

    @Test
    public void testGenerator() {

        final HashSet<String> uniquelyGeneratedAddresses = new HashSet<>();
        final JobcoinAddressGenerator generator = new JobcoinAddressGenerator();
        for (int i = 0; i < 100; i++) {
            final String address = generator.generateAddress();
            final char[] chars = address.toCharArray();
            Assert.assertEquals(chars.length, 32);
            final HashSet<Character> uniqueChars = new HashSet<>();
            for (char c : chars) {
                uniqueChars.add(c);
            }
            Assert.assertTrue(uniqueChars.size() > 1);
            Assert.assertFalse(uniquelyGeneratedAddresses.contains(address));
            uniquelyGeneratedAddresses.add(address);
        }
    }
}
