/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cbus.handler;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverx.socket.cgate.CGateConnException;
import serverx.socket.cgate.CGateTimeOutException;

/**
 * The {@link CBusDaliHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public class CBusDaliHandler extends CBusGroupHandler {

    private Logger logger = LoggerFactory.getLogger(CBusDaliHandler.class);

    public CBusDaliHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CBusBindingConstants.CHANNEL_LEVEL)) {
            logger.info("Channel command {}: {}", channelUID.getAsString(), command.toString());
            try {
                if (command instanceof OnOffType) {
                    if (command.equals(OnOffType.ON)) {
                        commandSet.turnOn(cBusNetworkHandler.getNetworkID(), CBusBindingConstants.CBUS_APPLICATION_DALI,
                                getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString());
                    } else if (command.equals(OnOffType.OFF)) {
                        commandSet.turnOff(cBusNetworkHandler.getNetworkID(),
                                CBusBindingConstants.CBUS_APPLICATION_DALI,
                                getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString());
                    }
                } else if (command instanceof PercentType) {
                    PercentType value = (PercentType) command;
                    logger.info("DALI: {} {} {} {} {}", cBusNetworkHandler.getNetworkID(),
                            CBusBindingConstants.CBUS_APPLICATION_DALI,
                            getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString(),
                            Integer.toString((int) Math.round(value.doubleValue())) + "%", "");
                    // commandSet.ramp(cBusNetworkHandler.getNetworkID(), CBusBindingConstants.CBUS_APPLICATION_DALI,
                    // getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString(),
                    // Integer.toString((int) Math.round(value.doubleValue())) + "%", "");
                    cBusCGateHandler.ramp(cBusNetworkHandler.getNetworkID(), CBusBindingConstants.CBUS_APPLICATION_DALI,
                            getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString(),
                            Integer.toString((int) Math.round(value.doubleValue())) + "%", "");
                } else if (command instanceof IncreaseDecreaseType) {
                    logger.warn("Increase/Decrease not implemented for {}", channelUID.getAsString());
                }
            } catch (CGateConnException | CGateTimeOutException e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

}
