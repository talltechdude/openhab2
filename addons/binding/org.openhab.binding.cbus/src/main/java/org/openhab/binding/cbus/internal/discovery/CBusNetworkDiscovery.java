package org.openhab.binding.cbus.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.openhab.binding.cbus.handler.CBusCGateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CBusNetworkDiscovery extends AbstractDiscoveryService {

    private final static Logger logger = LoggerFactory.getLogger(CBusNetworkDiscovery.class);

    private CBusCGateHandler cgateHandler;

    public CBusNetworkDiscovery(CBusCGateHandler cgateHandler) {
        super(CBusBindingConstants.NETWORK_DISCOVERY_THING_TYPES_UIDS, 60, false);
        this.cgateHandler = cgateHandler;
    }

    @Override
    protected void startScan() {
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
