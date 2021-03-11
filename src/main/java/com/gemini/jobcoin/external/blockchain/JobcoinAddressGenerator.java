package com.gemini.jobcoin.external.blockchain;

import java.security.SecureRandom;

/**
 * This class is a naive implementation of a address generator.
 *
 * A more sophisticated implementation would require the Jobcoin API/blockchain
 * to generate a deposit address which is guaranteed to be unique and follows Jobcoin
 * address constraints (if any exist).
 *
 * Additionally the addresses created over time would most likely be offloaded from
 * this app to prevent continued memory growth. The Jobcoin API/blockchain itself would
 * manage all created addresses.
 */
public class JobcoinAddressGenerator implements AddressGenerator {

    public static final String JOBCOIN_HOUSE_ADDRESS = "JOBCOINHOUSEADDRESS";

    private static final String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final SecureRandom rnd = new SecureRandom();

    /**
     * Creates a deposit address ensuring randomness.
     *
     * This could be expanded to protect against the unlikelihood that a collision
     * arises by trying to insert it atomically using putIfAbsent and trying again
     * if it returns not null (which would indicate the address is there already).
     *
     * @return Jobcoin generated depositAddress
     */
    @Override
    public String generateAddress() {
        return randomString(32);
    }

    /**
     * Creates a random string of specific length
     *
     * @param len to create string
     * @return randomString
     */
    private String randomString(int len){
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++) {
            sb.append(charset.charAt(rnd.nextInt(charset.length())));
        }
        return sb.toString();
    }
}
