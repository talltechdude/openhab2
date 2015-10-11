/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.controlbyweb.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.controlbyweb.config.ControlByWebConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The {@link ControlByWebHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public class ControlByWebHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(ControlByWebHandler.class);

    private InetAddress ipAddress;

    private static final int DEFAULT_REFRESH_INTERVAL = 60;
    private ScheduledFuture<?> pollingJob;

    private Runnable pollingRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                updateDeviceStatus();
            } catch (Exception e) {
                logger.debug("Exception during poll : {}", e);
            }
        }
    };

    public ControlByWebHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().startsWith("relay")) {
            String relayNumber = channelUID.getId().replace("relay", "");
            String url = "http://" + ipAddress.getHostAddress() + "/state.xml?relay" + relayNumber + "State=";
            if (command.equals(OnOffType.ON)) {
                url += "1";
            } else if (command.equals(OnOffType.OFF)) {
                url += "0";
            }
            Scanner scanner = null;
            try {
                scanner = new Scanner(new URL(url).openStream(), "UTF-8");
                if (scanner.hasNext()) {
                    String response = scanner.useDelimiter("\\A").next();
                    logger.trace("Turning {} relay {}: {}", command.toString(), relayNumber, response);
                }
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot execute relay command");
                logger.debug("Exception executing relay command: {}", e);
            } finally {
                if (scanner != null)
                    scanner.close();
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing ControlByWeb handler.");
        ControlByWebConfiguration configuration = getConfigAs(ControlByWebConfiguration.class);
        logger.debug("ControlByWeb IP {}.", configuration.ipAddress);
        if (configuration.ipAddress == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Can not access device. IP address \"" + configuration.ipAddress + "\" is invalid");
            logger.warn("Can not access device. IP address \"" + configuration.ipAddress + "\" is invalid");
            return;
        } else if ("127.0.0.1".equals(configuration.ipAddress) || "localhost".equals(configuration.ipAddress)) {
            ipAddress = InetAddress.getLoopbackAddress();
        } else {
            try {
                ipAddress = InetAddress.getByName(configuration.ipAddress);
            } catch (UnknownHostException e1) {
                updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                        "IP Address not resolvable");
                return;
            }
        }

        if (pollingJob == null || pollingJob.isCancelled()) {
            // use default refresh interval if not specified
            int refreshInterval = DEFAULT_REFRESH_INTERVAL;
            if (configuration.refresh != null) {
                refreshInterval = configuration.refresh;
            }
            pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0, refreshInterval, TimeUnit.SECONDS);
        }
    }

    protected void updateDeviceStatus() throws Exception {
        String uri = "http://" + ipAddress.getHostAddress() + "/state.xml";
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder;
        Document doc = null;
        try {
            builder = domFactory.newDocumentBuilder();
            doc = builder.parse(uri.toString());
        } catch (SAXException | IOException | ParserConfigurationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Cannot load status.xml from device");
            throw e;
        }
        if (doc.getElementsByTagName("datavalues").getLength() == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Cannot load status.xml from device");
        }
        updateStatus(ThingStatus.ONLINE);
        String inputStates = doc.getElementsByTagName("inputstates").item(0).getFirstChild().getTextContent();
        for (int i = 1; i < inputStates.length(); i++) {
            State state = new StringType(inputStates.charAt(inputStates.length() - i) == '1' ? OnOffType.ON.toString()
                    : OnOffType.OFF.toString());
            updateState(new ChannelUID(getThing().getUID(), "input" + i), state);
            logger.trace("Updating input {} to {}", i, state.toString());
        }

        String relayStates = doc.getElementsByTagName("relaystates").item(0).getFirstChild().getTextContent();
        for (int i = 1; i <= relayStates.length(); i++) {
            State state = relayStates.charAt(relayStates.length() - i) == '1' ? OnOffType.ON : OnOffType.OFF;
            updateState(new ChannelUID(getThing().getUID(), "relay" + i), state);
            logger.trace("Updating relay {} to {}", i, state.toString());
        }
    }

}
