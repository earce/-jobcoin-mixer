package com.gemini.jobcoin.external.persistence;

import java.util.UUID;

/**
 * Class is a wrapper around Java's UUID string generator. This again
 * is intended to represent a system that would be able to coordinate
 * creation of request ids across multiple instances of the system.
 *
 * It also helps the team supporting this application as it facilitates
 * tracking down the state of a mixing request. This is especially true
 * if this is stored in a persistent store and the application crashed.
 *
 * Otherwise with no handle on the request it would make tying out the
 * request with the state difficult. The deposit address uniqueness would
 * not be sufficient because users could send money to the deposit address
 * as two different requests.
 */
public class RequestIdGenerator implements UUIDGenerator {

    /**
     * Creates a UUID to identify a request
     *
     * This could be expanded to protect against the unlikelihood that a collision
     * arises (mathematically speaking this is extremely difficult) by keeping a
     * map of all seen UUIDs and attempting to insert a new one atomically using
     * putIfAbsent and trying again if it returns not null (which would indicate
     * the UUID is there already)
     *
     * @return Jobcoin generated request id
     */
    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
