/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.cbus.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverx.socket.cgate.CGateCommandSet;

/**
 * The {@link CBusGroupHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public abstract class CBusGroupHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(CBusGroupHandler.class);
    protected CBusNetworkHandler cBusNetworkHandler = null;
    protected CBusCGateHandler cBusCGateHandler = null;
    protected CGateCommandSet commandSet = null;

    public CBusGroupHandler(Thing thing) {
        super(thing);
    }

    @Override
    public abstract void handleCommand(ChannelUID channelUID, Command command);

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);
        cBusNetworkHandler = getCBusNetworkHandler();
        cBusCGateHandler = cBusNetworkHandler.getCBusCGateHandler();
        commandSet = cBusCGateHandler.getCommandSet();
        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    private synchronized CBusNetworkHandler getCBusNetworkHandler() {
        CBusNetworkHandler bridgeHandler = null;
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Required bridge not defined for device {}.");
            return null;
        }
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof CBusNetworkHandler) {
            bridgeHandler = (CBusNetworkHandler) handler;
        } else {
            logger.debug("No available bridge handler found for bridge: {} .", bridge.getUID());
            bridgeHandler = null;
        }
        return bridgeHandler;
    }
}
