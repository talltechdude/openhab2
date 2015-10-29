/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cbus.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CBusTriggerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public class CBusTriggerHandler extends CBusGroupHandler {

    private Logger logger = LoggerFactory.getLogger(CBusTriggerHandler.class);

    public CBusTriggerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CBusBindingConstants.CHANNEL_VALUE)) {
            logger.info("Channel command {}: {}", channelUID.getAsString(), command.toString());
            if (command instanceof OnOffType) {
                if (command.equals(OnOffType.ON)) {
                    cBusCGateHandler.trigger(cBusNetworkHandler.getNetworkID(),
                            getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString(), "255");
                } else if (command.equals(OnOffType.OFF)) {
                    cBusCGateHandler.trigger(cBusNetworkHandler.getNetworkID(),
                            getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString(), "0");
                }
            } else if (command instanceof DecimalType) {
                cBusCGateHandler.trigger(cBusNetworkHandler.getNetworkID(),
                        getConfig().get(CBusBindingConstants.CONFIG_GROUP_ID).toString(), command.toString());
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

}
