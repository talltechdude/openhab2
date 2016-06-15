package org.openhab.binding.networkthermostat.config;

import java.math.BigDecimal;

public class NetworkThermostatConfiguration {
    public static final String IP_ADDRESS = "ipAddress";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String REFRESH = "refresh";
    public static final String TIMEOUT = "timeout";
    public static final String READONLY = "readonly";

    public String ipAddress;
    public String username;
    public String password;
    public BigDecimal refresh;
    public BigDecimal timeout;
    public Boolean readonly;
}
