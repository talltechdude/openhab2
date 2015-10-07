/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.controlbyweb.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.controlbyweb.ControlByWebBindingConstants;
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
        if (channelUID.getId().equals(ControlByWebBindingConstants.CHANNEL_OUTPUT_1)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.INITIALIZING);
        logger.debug("Initializing ControlByWeb handler.");
        ControlByWebConfiguration configuration = getConfigAs(ControlByWebConfiguration.class);
        logger.debug("ControlByWeb IP {}.", configuration.ipAddress);
        if (configuration.ipAddress == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Can not access device as ip address is invalid");
            logger.warn("Can not access device as ip address is invalid");
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
            Configuration config = getThing().getConfiguration();
            // use default if not specified
            int refreshInterval = DEFAULT_REFRESH_INTERVAL;
            Object refreshConfig = config.get(ControlByWebConfiguration.REFRESH);
            if (refreshConfig != null) {
                refreshInterval = ((BigDecimal) refreshConfig).intValue();
            }
            pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0, refreshInterval, TimeUnit.SECONDS);
        }

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    protected void updateDeviceStatus() throws URISyntaxException {

        // UriBuilder uriBuilder = UriBuilder.fromPath(ipAddress.toString()).scheme("http").path("state.xml");

        String uri = "http://" + ipAddress.getHostAddress() + "/state.xml";
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder;
        Document doc = null;
        try {
            builder = domFactory.newDocumentBuilder();

            doc = builder.parse(uri.toString());
        } catch (SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String inputStates = doc.getElementsByTagName("inputstates").item(0).getFirstChild().getTextContent();

        String relayStates = doc.getElementsByTagName("relaystates").item(0).getFirstChild().getTextContent();

        // XPath xpath = XPathFactory.newInstance().newXPath();
        // XPath Query for showing all nodes value

    }
}
