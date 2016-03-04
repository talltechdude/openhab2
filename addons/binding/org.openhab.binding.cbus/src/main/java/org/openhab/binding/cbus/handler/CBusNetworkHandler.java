package org.openhab.binding.cbus.handler;

import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.openhab.binding.cbus.internal.cgate.CGateException;
import org.openhab.binding.cbus.internal.cgate.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CBusNetworkHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private CBusCGateHandler bridgeHandler;
    private Network network;

    public CBusNetworkHandler(Bridge thing) {
        super(thing);
        if (thing == null) {
            logger.error("Required bridge not defined for device.");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        getCBusCGateHandler();
        String networkID = getConfig().get(CBusBindingConstants.PROPERTY_NETWORK_ID).toString();
        String project = getConfig().get(CBusBindingConstants.PROPERTY_PROJECT).toString();
        try {
            network = (Network) getCBusCGateHandler().getCGateSession()
                    .getCGateObject("//" + project + "/" + networkID);
        } catch (CGateException e) {
            logger.error("Cannot load C-Bus network {}", networkID, e);
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.COMMUNICATION_ERROR);
        }
        updateStatus();
        scheduler.scheduleAtFixedRate(networkSyncRunnable, (int) (60 * Math.random()),
                Integer.parseInt(getConfig().get(CBusBindingConstants.PROPERTY_NETWORK_SYNC).toString()),
                TimeUnit.SECONDS);
    }

    public void updateStatus() {
        try {
            if (!getCBusCGateHandler().getThing().getStatus().equals(ThingStatus.ONLINE)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            } else if (network == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            } else if (network.isOnline()) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            }
        } catch (CGateException e) {
            logger.error("Problem checking network state for network {}",
                    network != null ? network.getNetworkID() : "<unknown>", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }

    public synchronized CBusCGateHandler getCBusCGateHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                logger.error("Required bridge not defined for device.");
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

    public Network getNetwork() {
        return network;
    }

    private Runnable networkSyncRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                logger.info("Starting network sync on network {}", network.getNetworkID());
                getNetwork().startSync();
            } catch (CGateException e) {
                logger.error("Cannot start network sync on {}", network.getNetworkID(), e);
            }
        }
    };
}
