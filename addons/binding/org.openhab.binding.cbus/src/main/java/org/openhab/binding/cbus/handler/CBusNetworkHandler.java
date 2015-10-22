package org.openhab.binding.cbus.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CBusNetworkHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(CBusGroupHandler.class);
    private CBusCGateHandler cBusCGateHandler;
    private int networkID;

    public CBusNetworkHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        cBusCGateHandler = getBridgeHandler();
        Configuration configuration = getConfig();
        networkID = Integer.parseInt(configuration.get(CBusBindingConstants.PROPERTY_NETWORK_ID).toString());
        cBusCGateHandler.registerNetworkHandler(this);
        updateStatus();

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        // updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    public void updateStatus() {
        if (!cBusCGateHandler.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            updateStatus(ThingStatus.OFFLINE);
        } else {
            if (cBusCGateHandler.getCommandSocket().isNetworkOnline(networkID)) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        }
    }

    public synchronized CBusCGateHandler getBridgeHandler() {
        CBusCGateHandler bridgeHandler = null;
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Required bridge not defined for device {}.");
            return null;
        }
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof CBusCGateHandler) {
            bridgeHandler = (CBusCGateHandler) handler;
        } else {
            logger.debug("No available bridge handler found for bridge: {} .", bridge.getUID());
            bridgeHandler = null;
        }
        return bridgeHandler;
    }

    public int getNetworkID() {
        return networkID;
    }

}
