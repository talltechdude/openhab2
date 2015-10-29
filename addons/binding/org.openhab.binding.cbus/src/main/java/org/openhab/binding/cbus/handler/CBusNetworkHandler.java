package org.openhab.binding.cbus.handler;

import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverx.socket.cgate.CGateConnException;
import serverx.socket.cgate.CGateTimeOutException;

public class CBusNetworkHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(CBusGroupHandler.class);
    private CBusCGateHandler bridgeHandler;
    private String networkID;

    public CBusNetworkHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        getCBusCGateHandler();
        networkID = getConfig().get(CBusBindingConstants.PROPERTY_NETWORK_ID).toString();
        updateStatus();
        scheduler.scheduleAtFixedRate(networkSyncRunnable, (int) (60 * Math.random()),
                Integer.parseInt(getConfig().get(CBusBindingConstants.PROPERTY_NETWORK_SYNC).toString()),
                TimeUnit.SECONDS);
    }

    public void updateStatus() {
        if (!getCBusCGateHandler().getThing().getStatus().equals(ThingStatus.ONLINE)) {
            updateStatus(ThingStatus.OFFLINE);
        } else {
            if (getCBusCGateHandler().isNetworkOnline(networkID)) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        }
    }

    public synchronized CBusCGateHandler getCBusCGateHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                logger.debug("Required bridge not defined for device {}.");
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof CBusCGateHandler) {
                this.bridgeHandler = (CBusCGateHandler) handler;
            } else {
                logger.debug("No available bridge handler found for bridge: {} .", bridge.getUID());
                this.bridgeHandler = null;
            }
        }
        return bridgeHandler;
    }

    public String getNetworkID() {
        return networkID;
    }

    private Runnable networkSyncRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                logger.info("Starting network sync on network {}", networkID);
                getCBusCGateHandler().getCommandSet().doNetworkSync(networkID);
            } catch (CGateConnException | CGateTimeOutException e) {
            }
        }
    };
}
