package com.gemini.jobcoin.constant;

public class Routes {

    // HTTP routes
    public static final String STATUS = "/status";

    public static final String REGISTER_V1 = "/v1/register";
    public static final String SEND_V1 = "/v1/send";
    public static final String BALANCE_V1 = "/v1/balance";
    public static final String COMMANDS_V1 = "/v1/commands";
    public static final String MIXING_STATUS_V1 = "/v1/mixingStatus";

    // Vertx routes
    public static final String REGISTER_VERTX_V1 = "/v1/route/register";
    public static final String SEND_VERTX_V1 = "/v1/route/send";
    public static final String BALANCE_VERTX_V1 = "/v1/route/balance";
    public static final String MIXER_VERTX_V1 = "/v1/route/mixer";
    public static final String MIXING_STATUS_VERTX_V1 = "/v1/route/mixingStatus";

}
