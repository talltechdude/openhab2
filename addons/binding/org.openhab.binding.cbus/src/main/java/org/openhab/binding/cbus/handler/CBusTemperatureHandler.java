/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cbus.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CBusTemperatureHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public class CBusTemperatureHandler extends CBusGroupHandler {

    private Logger logger = LoggerFactory.getLogger(CBusTemperatureHandler.class);

    public CBusTemperatureHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Read only thing - no commands to handle
    }

    @Override
    public void initialize() {
        super.initialize();
    }
}
