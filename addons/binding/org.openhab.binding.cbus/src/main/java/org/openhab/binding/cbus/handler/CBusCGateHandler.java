package org.openhab.binding.cbus.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.cbus.CBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import serverx.socket.cgate.CGateCommandSet;
import serverx.socket.cgate.CGateCommandSock;
import serverx.socket.cgate.CGateConnException;
import serverx.socket.cgate.CGateEventsSock;
import serverx.socket.cgate.CGateReconnect;
import serverx.socket.cgate.CGateReconnectEvent;
import serverx.socket.cgate.CGateTimeOutException;

public class CBusCGateHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(CBusCGateHandler.class);

    private InetAddress ipAddress;

    // private CGateConnection cgate;

    // private CGateCommandSocket commandSocket;

    private CGateCommandSock commandSock;
    private CGateReconnect commandSockReconnect;

    private CGateEventsSock statusSock;
    private CGateReconnectEvent statusSockReconnect;

    private CGateEventsSock eventsSock;
    private CGateReconnectEvent eventsSockReconnect;

    private boolean shutdownRequested = false;

    public CBusCGateHandler(Bridge br) {
        super(br);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub
    }

    @Override
    public void initialize() {
        super.initialize();
        updateStatus(ThingStatus.INITIALIZING);
        logger.debug("Initializing CGate Bridge handler.");
        Configuration config = getThing().getConfiguration();
        String ipAddress = (String) config.get(CBusBindingConstants.PROPERTY_IP_ADDRESS);

        if ("127.0.0.1".equals(ipAddress) || "localhost".equals(ipAddress)) {
            this.ipAddress = InetAddress.getLoopbackAddress();
        } else {
            try {
                this.ipAddress = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e1) {
                updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                        "IP Address not resolvable");
                return;
            }
        }
        // if (config.get(CBusBindingConstants.PROPERTY_REFRESH_INTERVAL) != null) {

        logger.debug("CGate IP         {}.", this.ipAddress.getHostAddress());

        shutdownRequested = false;
        commandSock = new CGateCommandSock();
        commandSock.setServerName(this.ipAddress.getHostAddress());
        commandSock.setTimeOut(500);
        commandSockReconnect = new CGateReconnect(commandSock);
        commandSockReconnect.start();

        eventsSock = new CGateEventsSock();
        eventsSock.setServerName(this.ipAddress.getHostAddress());
        eventsSock.setPortNo(20024);
        eventsSockReconnect = new CGateReconnectEvent(eventsSock);
        eventsSockReconnect.start();
        eventsMonitor.start();

        statusSock = new CGateEventsSock();
        statusSock.setServerName(this.ipAddress.getHostAddress());
        statusSock.setPortNo(20025);
        statusSockReconnect = new CGateReconnectEvent(statusSock);
        statusSockReconnect.start();
        statusMonitor.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        updateStatus();
        // cgate = new CGateConnection(configuration.ipAddress);
        try {
            // cgate.connect();

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;

        }
    }

    public synchronized CGateCommandSet getCommandSet() {
        try {
            if (commandSock == null)
                return null;
            return new CGateCommandSet(commandSock);
        } catch (CGateConnException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        shutdownRequested = true;
        if (commandSockReconnect != null) {
            commandSockReconnect.stop();
        }
        if (eventsSockReconnect != null) {
            eventsSockReconnect.stop();
        }
        if (statusSockReconnect != null) {
            statusSockReconnect.stop();
        }

        super.dispose();
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    public void updateStatus() {
        if (commandSock.noop()) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    private Thread eventsMonitor = new Thread() {
        @Override
        public void run() {
            this.setName("Event socket monitor");
            while (!shutdownRequested) {
                try {
                    String event = eventsSock.readEvents();
                    if (event != null && !event.equals("-1")) {
                        parseEvent(event);
                        continue;
                    }
                    Thread.sleep(100L);
                } catch (Exception e) {

                }
            }
        }
    };

    private Thread statusMonitor = new Thread() {
        @Override
        public void run() {
            this.setName("Status socket monitor");
            while (!shutdownRequested) {
                try {
                    String status = statusSock.readEvents();
                    if (status != null && !status.equals("-1")) {
                        parseStatus(status);
                        continue;
                    }
                    Thread.sleep(100L);
                } catch (Exception e) {

                }
            }
        }
    };

    private void parseEvent(String event) {
        logger.debug("Received event {}", event);
        String contents[] = event.split("#");
        LinkedList<String> tokenizer = new LinkedList<String>(Arrays.asList(contents[0].split("\\s+")));
        // 701
        tokenizer.poll();
        if (tokenizer.peek().equals("701")) {
            tokenizer.poll();
            String address = tokenizer.poll();
            String oid = tokenizer.poll();
            String value = tokenizer.poll();
            if (value.startsWith("level=")) {
                String level = value.replace("level=", "");
                updateGroup(address, level);
            }
        }
    }

    private void parseStatus(String status) {
        String contents[] = status.split("#");
        LinkedList<String> tokenizer = new LinkedList<String>(Arrays.asList(contents[0].split("\\s+")));
        LinkedList<String> commentTokenizer = new LinkedList<String>(Arrays.asList(contents[1].split("\\s+")));

        if (tokenizer.peek().equals("lighting")) {
            tokenizer.poll();
            String state = tokenizer.poll();
            String address = tokenizer.poll();
            if (state.equals("ramp"))
                state = tokenizer.poll();
            updateGroup(address, state);
        } else if (tokenizer.peek().equals("temperature")) {
            tokenizer.poll();
            tokenizer.poll();
            String address = tokenizer.poll();
            String temp = tokenizer.poll();
            updateGroup(address, temp);
        } else if (tokenizer.peek().equals("trigger")) {
            tokenizer.poll();
            tokenizer.poll();
            String address = tokenizer.poll();
            String level = tokenizer.poll();
            updateGroup(address, level);
        } else if (tokenizer.peek().equals("clock")) {
            tokenizer.poll();
            String address = "";
            String value = "";
            String type = tokenizer.poll();
            if (type.equals("date")) {
                address = tokenizer.poll() + "/1";
                value = tokenizer.poll();
            } else if (type.equals("time")) {
                address = tokenizer.poll() + "/0";
                value = tokenizer.poll();
            } else if (type.equals("request_refresh")) {

            } else {
                logger.error("Received unknown clock event: {}", status);
            }
            if (value != "") {
                updateGroup(address, value);
            }
        } else if (commentTokenizer.peek().equals("lighting")) {
            commentTokenizer.poll();
            if (commentTokenizer.peek().equals("SyncUpdate")) {
                commentTokenizer.poll();
                String address = commentTokenizer.poll();
                String level = commentTokenizer.poll().replace("level=", "");
                updateGroup(address, level);
            }
        } else {
            logger.info("Received unparsed event: '{}'", status);
        }
    }

    private void updateGroup(String address, String value) {
        String[] addressParts = address.trim().replace("//EVCHURCH/", "").split("/");
        updateGroup(addressParts[0], addressParts[1], addressParts[2], value);
    }

    private void updateGroup(String network, String application, String group, String value) {
        boolean handled = false;
        for (Thing networkThing : getThing().getThings()) {
            // Is this networkThing from the network we are looking for...
            if (networkThing.getThingTypeUID().equals(CBusBindingConstants.BRIDGE_TYPE_NETWORK) && networkThing
                    .getConfiguration().get(CBusBindingConstants.PROPERTY_NETWORK_ID).toString().equals(network)) {

                // Loop through all the things on this network and see if they match the application / group
                for (Thing thing : ((CBusNetworkHandler) networkThing.getHandler()).getThing().getThings()) {
                    // Handle Lighting Application messages
                    if (application.equals(CBusBindingConstants.CBUS_APPLICATION_LIGHTING)
                            && thing.getThingTypeUID().equals(CBusBindingConstants.THING_TYPE_LIGHT)
                            && thing.getConfiguration().get(CBusBindingConstants.CONFIG_GROUP_ID).toString()
                                    .equals(group)) {

                        ChannelUID channelUID = thing.getChannel(CBusBindingConstants.CHANNEL_STATE).getUID();

                        if ("on".equalsIgnoreCase(value) || "255".equalsIgnoreCase(value))
                            updateState(channelUID, OnOffType.ON);
                        else if ("off".equalsIgnoreCase(value) || "0".equalsIgnoreCase(value))
                            updateState(channelUID, OnOffType.OFF);
                        else {
                            try {
                                int v = Integer.parseInt(value);
                                updateState(channelUID, v > 0 ? OnOffType.ON : OnOffType.OFF);
                            } catch (NumberFormatException e) {
                                logger.error("Invalid value presented to channel {}. Received {}, expected On/Off",
                                        channelUID, value);
                            }
                        }
                        handled = true;
                        logger.debug("Updating CBus Lighting Group {} with value {}", thing.getUID(), value);

                    }
                    // DALI Application
                    else if (application.equals(CBusBindingConstants.CBUS_APPLICATION_DALI)
                            && thing.getThingTypeUID().equals(CBusBindingConstants.THING_TYPE_DALI)
                            && thing.getConfiguration().get(CBusBindingConstants.CONFIG_GROUP_ID).toString()
                                    .equals(group)) {

                        ChannelUID channelUID = thing.getChannel(CBusBindingConstants.CHANNEL_LEVEL).getUID();

                        if ("on".equalsIgnoreCase(value) || "255".equalsIgnoreCase(value)) {
                            updateState(channelUID, OnOffType.ON);
                            updateState(channelUID, new PercentType(100));
                        } else if ("off".equalsIgnoreCase(value) || "0".equalsIgnoreCase(value)) {
                            updateState(channelUID, OnOffType.OFF);
                            updateState(channelUID, new PercentType(0));
                        } else {
                            try {
                                int v = Integer.parseInt(value);
                                PercentType perc = new PercentType(Math.round(v * 100 / 255));
                                updateState(channelUID, perc);
                            } catch (NumberFormatException e) {
                                logger.error(
                                        "Invalid value presented to channel {}. Received {}, expected On/Off or decimal value",
                                        channelUID, value);
                            }
                        }
                        handled = true;
                        logger.debug("Updating CBus Lighting Group {} with value {}", thing.getUID(), value);

                    }
                    // Temperature Application
                    else if (application.equals(CBusBindingConstants.CBUS_APPLICATION_TEMPERATURE)
                            && thing.getThingTypeUID().equals(CBusBindingConstants.THING_TYPE_TEMPERATURE)
                            && thing.getConfiguration().get(CBusBindingConstants.CONFIG_GROUP_ID).toString()
                                    .equals(group)) {

                        ChannelUID channelUID = thing.getChannel(CBusBindingConstants.CHANNEL_TEMP).getUID();
                        DecimalType temp = new DecimalType(value);
                        updateState(channelUID, temp);
                        logger.trace("Updating CBus Temperature Group {} with value {}", thing.getUID(), value);
                        handled = true;
                    }
                    // Trigger Application
                    else if (application.equals(CBusBindingConstants.CBUS_APPLICATION_TRIGGER)
                            && thing.getThingTypeUID().equals(CBusBindingConstants.THING_TYPE_TRIGGER)
                            && thing.getConfiguration().get(CBusBindingConstants.CONFIG_GROUP_ID).toString()
                                    .equals(group)) {

                        ChannelUID channelUID = thing.getChannel(CBusBindingConstants.CHANNEL_VALUE).getUID();
                        DecimalType val = new DecimalType(value);
                        // updateState(channelUID, val);
                        logger.trace("Updating CBus Trigger Group {} with value {}", thing.getUID(), value);
                        handled = true;
                    }
                }
            }
        }
        if (!handled)
            // logger.warn("Unhandled CBus value update for {}/{}/{}: {}", network, application, group, value);
            ;
        else
            logger.trace("CBus value update for {}/{}/{}: {}", network, application, group, value);
    }

    public void trigger(String network, String group, String value) {
        commandSock.sendCommand("TRIGGER EVENT //EVCHURCH/" + network + "/"
                + CBusBindingConstants.CBUS_APPLICATION_TRIGGER + "/" + group + " " + value);
    }

    public void ramp(String network, String application, String group, String value, String time) {
        logger.info("RAMPING:{}", commandSock.sendCommand(
                "RAMP //EVCHURCH/" + network + "/" + application + "/" + group + " " + value + " " + time));
    }

    public boolean isNetworkOnline(String network) {
        try {
            return "OK".equals(getCommandSet().isNetworkStateOK(network));
        } catch (CGateConnException | CGateTimeOutException e) {
            return false;
        }
    }
}