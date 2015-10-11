package org.openhab.binding.networkthermostat.internal;

import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.PrimitiveType;
import org.eclipse.smarthome.core.types.State;

/**
 * This enumeration represents the different mode types of a Network Thermostat.
 * 
 * @author Scott Linton
 */
public enum ThermostatModeType implements PrimitiveType,State,Command {
    AUTO,
    COOL,
    HEAT,
    EHEAT,
    OFF;

    @Override
    public String format(String pattern) {
        return String.format(pattern, this.toString());
    }
}
