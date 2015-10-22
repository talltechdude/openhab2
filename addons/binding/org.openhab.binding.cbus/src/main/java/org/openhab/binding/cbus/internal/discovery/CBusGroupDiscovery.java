package org.openhab.binding.cbus.internal.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.openhab.binding.cbus.handler.CBusNetworkHandler;
import org.openhab.binding.cbus.internal.cgate.CGateCommandSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CBusGroupDiscovery extends AbstractDiscoveryService {

    private final static Logger logger = LoggerFactory.getLogger(CBusGroupDiscovery.class);

    private CBusNetworkHandler cbusNetworkHandler;

    public CBusGroupDiscovery(CBusNetworkHandler cbusNetworkHandler) {
        super(CBusBindingConstants.SUPPORTED_THING_TYPES_UIDS, 10, false);
        this.cbusNetworkHandler = cbusNetworkHandler;
    }

    // public CBusNetworkDiscovery() {
    // super(CBusBindingConstants.SUPPORTED_THING_TYPES_UIDS, 10, true);
    // }

    @Override
    protected void startScan() {
        scanForGroups();
    }

    private void scanForGroups() {
        if (cbusNetworkHandler.getThing().getStatus().equals(ThingStatus.ONLINE)) {
            CGateCommandSocket commandSocket = cbusNetworkHandler.getBridgeHandler().getCommandSocket();
            // Retrieve list of networks from CGate
            Map<Integer, String> groups = commandSocket.listGroups(cbusNetworkHandler.getNetworkID(), 56);
            for (Map.Entry<Integer, String> group : groups.entrySet()) {
                logger.info("Found Group: {} {}", group.getKey(), group.getValue());
                Map<String, Object> properties = new HashMap<>(2);
                properties.put(CBusBindingConstants.CONFIG_GROUP_ID, group.getKey());
                properties.put(CBusBindingConstants.PROPERTY_NETWORK_NAME, group.getValue());
                ThingUID uid = new ThingUID(CBusBindingConstants.THING_TYPE_GROUP, group.getKey().toString());
                if (uid != null) {
                    DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                            .withLabel("C-Bus Group " + group.getKey() + " - " + group.getValue())
                            .withBridge(cbusNetworkHandler.getThing().getUID()).build();
                    thingDiscovered(result);
                }
            }
        }
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

    // private void discoveryResultSubmission(String network) {
    // if (network != null) {
    // logger.info("Adding new C-Bus Network {} to Smarthome inbox", network);
    // Map<String, Object> properties = new HashMap<>(2);
    // properties.put(CBusBindingConstants.PROPERTY_NETWORK_ID, network);
    // ThingUID uid = new ThingUID(CBusBindingConstants.THING_TYPE_GROUP, network);
    // if (uid != null) {
    // DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
    // .withLabel("C-Bus Group " + network).withBridge(cbusNetworkHandler.getThing().getUID()).build();
    // thingDiscovered(result);
    // }
    // }
    // }

}
