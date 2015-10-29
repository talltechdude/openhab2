/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cbus.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverx.socket.cgate.CGateConnException;
import serverx.socket.cgate.CGateTimeOutException;

/**
 * The {@link CBusLightHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public class CBusLightHandler extends CBusGroupHandler {

    private Logger logger = LoggerFactory.getLogger(CBusLightHandler.class);

    public CBusLightHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CBusBindingConstants.CHANNEL_STATE)) {
            logger.info("Channel command {}: {}", channelUID.getAsString(), command.toString());
            if (command instanceof OnOffType) {
                try {
                    if (command.equals(OnOffType.ON)) {
                        commandSet.turnOn(cBusNetworkHandler.getNetworkID(),
                                CBusBindingConstants.CBUS_APPLICATION_LIGHTING,
                                getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString());
                    } else if (command.equals(OnOffType.OFF)) {
                        commandSet.turnOff(cBusNetworkHandler.getNetworkID(),
                                CBusBindingConstants.CBUS_APPLICATION_LIGHTING,
                                getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString());
                    }
                } catch (CGateConnException | CGateTimeOutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

}
