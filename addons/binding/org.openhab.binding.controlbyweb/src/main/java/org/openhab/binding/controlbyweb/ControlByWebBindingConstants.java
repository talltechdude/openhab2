/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.controlbyweb;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ControlByWebBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Scott Linton - Initial contribution
 */
public class ControlByWebBindingConstants {

    public static final String BINDING_ID = "controlbyweb";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_X332 = new ThingTypeUID(BINDING_ID, "x332");

    // List of all Channel ids
    public final static String CHANNEL_INPUT_1 = "input1";
    public final static String CHANNEL_INPUT_2 = "input2";
    public final static String CHANNEL_INPUT_3 = "input3";
    public final static String CHANNEL_INPUT_4 = "input4";
    public final static String CHANNEL_INPUT_5 = "input5";
    public final static String CHANNEL_INPUT_6 = "input6";

    public final static String CHANNEL_OUTPUT_1 = "output1";
    public final static String CHANNEL_OUTPUT_2 = "output2";
    public final static String CHANNEL_OUTPUT_3 = "output3";
    public final static String CHANNEL_OUTPUT_4 = "output4";
    public final static String CHANNEL_OUTPUT_5 = "output5";
    public final static String CHANNEL_OUTPUT_6 = "output6";
}
