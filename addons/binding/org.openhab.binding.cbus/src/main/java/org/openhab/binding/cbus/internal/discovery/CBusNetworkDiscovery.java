package org.openhab.binding.cbus.internal.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.openhab.binding.cbus.handler.CBusCGateHandler;
import org.openhab.binding.cbus.internal.cgate.CGateException;
import org.openhab.binding.cbus.internal.cgate.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CBusNetworkDiscovery extends AbstractDiscoveryService {

    private final static Logger logger = LoggerFactory.getLogger(CBusNetworkDiscovery.class);

    private CBusCGateHandler cBusCGateHandler;

    public CBusNetworkDiscovery(CBusCGateHandler cBusCGateHandler) {
        super(CBusBindingConstants.NETWORK_DISCOVERY_THING_TYPES_UIDS, 60, false);
        this.cBusCGateHandler = cBusCGateHandler;
    }

    @Override
    protected void startScan() {
        if (cBusCGateHandler.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            try {
                ArrayList<Network> networks = Network.listAll(cBusCGateHandler.getCGateSession(), false);
                for (Network network : networks) {
                    logger.info("Found Network: {} {}", network.getNetworkID(), network.getName());
                    Map<String, Object> properties = new HashMap<>(2);
                    properties.put(CBusBindingConstants.PROPERTY_ID, network.getNetworkID());
                    properties.put(CBusBindingConstants.PROPERTY_NAME, network.getName());
                    properties.put(CBusBindingConstants.PROPERTY_PROJECT, network.getProjectName());
                    ThingUID uid = new ThingUID(CBusBindingConstants.BRIDGE_TYPE_NETWORK,
                            network.getProjectName().toLowerCase().replace(" ", "_")
                                    + Integer.toString(network.getNetworkID()),
                            cBusCGateHandler.getThing().getUID().getId());
                    if (uid != null) {
                        DiscoveryResult result = DiscoveryResultBuilder.create(uid)
                                .withProperties(properties).withLabel(network.getProjectName() + "/"
                                        + network.getNetworkID() + " - " + network.getName())
                                .withBridge(cBusCGateHandler.getThing().getUID()).build();
                        thingDiscovered(result);
                    }
                }
            } catch (CGateException e) {
                logger.error("Failed to discover networks", e);
            }

        }

        // if (cgateHandler.getThing().getStatus().equals(ThingStatus.ONLINE)) {
        // CGateCommandSocket commandSocket = cgateHandler.getCommandSocket();
        // // Retrieve list of networks from CGate
        // Map<Integer, String> networks = commandSocket.listNetworks();
        // for (Map.Entry<Integer, String> network : networks.entrySet()) {
        // logger.info("Found Network: {} {}", network.getKey(), network.getValue());
        // Map<String, Object> properties = new HashMap<>(2);
        // properties.put(CBusBindingConstants.PROPERTY_NETWORK_ID, network.getKey());
        // properties.put(CBusBindingConstants.PROPERTY_NETWORK_NAME, network.getValue());
        // ThingUID uid = new ThingUID(CBusBindingConstants.BRIDGE_TYPE_NETWORK, network.getKey().toString());
        // if (uid != null) {
        // DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
        // .withLabel("C-Bus Network " + network.getKey() + " - " + network.getValue())
        // .withBridge(cgateHandler.getThing().getUID()).build();
        // thingDiscovered(result);
        // }
        // }
        // }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    @Override
    protected void startBackgroundDiscovery() {
        // TODO Auto-generated method stub
        super.startBackgroundDiscovery();
    }

    @Override
    protected void stopBackgroundDiscovery() {
        // TODO Auto-generated method stub
        super.stopBackgroundDiscovery();
    }

    public void activate() {
        // cgateHandler.registerDiscoveryService(this);
    }

    @Override
    public void deactivate() {
        // cgateHandler.unregisterDiscoveryService();
    }
}
