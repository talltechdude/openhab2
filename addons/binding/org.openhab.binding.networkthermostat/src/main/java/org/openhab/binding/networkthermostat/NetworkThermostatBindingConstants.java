/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.networkthermostat;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link NetworkThermostatBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Scott Linton - Initial contribution
 */
public class NetworkThermostatBindingConstants {

    public static final String BINDING_ID = "networkthermostat";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_THERMOSTAT = new ThingTypeUID(BINDING_ID, "thermostat");

    // List of all Channel ids
    public final static String CHANNEL_COOL_SETPOINT = "cool_setpoint";
    public final static String CHANNEL_HEAT_SETPOINT = "heat_setpoint";

    public final static String CHANNEL_MODE = "mode";
    public final static String CHANNEL_FAN_MODE = "fan_mode";
    public final static String CHANNEL_INDOOR_TEMP = "indoor_temp";
    public final static String CHANNEL_OUTDOOR_TEMP = "outdoor_temp";
    public final static String CHANNEL_COMPRESSOR_MODE = "compressor_mode";
    public final static String CHANNEL_COMPRESSOR_STAGE = "compressor_stage";
    public final static String CHANNEL_OVERRIDE_TIMER = "override_timer";
    public final static String CHANNEL_OVERRIDE = "override";
    public final static String CHANNEL_LED1 = "led1";
    public final static String CHANNEL_LED2 = "led2";
    /*
     * <channel id="cool_setpoint" typeId="setpoint"/>
     * <channel id="heat_setpoint" typeId="setpoint"/>
     * <channel id="mode" typeId="mode"/>
     * <channel id="indoor_temp" typeId="current_temp"/>
     * <channel id="outdoor_temp" typeId="current_temp"/>
     * <channel id="compressor_mode" typeId="compressor_mode"/>
     * <channel id="compressor_stage" typeId="compressor_stage"/>
     */

}
