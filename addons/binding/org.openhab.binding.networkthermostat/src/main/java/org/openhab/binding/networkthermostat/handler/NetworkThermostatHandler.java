/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.networkthermostat.handler;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.networkthermostat.NetworkThermostatBindingConstants;
import org.openhab.binding.networkthermostat.config.NetworkThermostatConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The {@link NetworkThermostatHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public class NetworkThermostatHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private InetAddress inetAddress;

    private static final int DEFAULT_REFRESH_INTERVAL = 60;

    private ScheduledFuture<?> pollingJob;

    private NetworkThermostatConfiguration configuration;

    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                pollThermostat();
            } catch (Exception e) {
                logger.debug("Exception during status poll: {}", e);
            }
        }
    };

    public NetworkThermostatHandler(Thing thing) {
        super(thing);
    }

    private void pollThermostat() {
        try {
            logger.debug("Beginning poll on " + getThing().getUID());
            URL url = new URL("http:/" + inetAddress + "/index.xml");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            String encoded = Base64.getEncoder()
                    .encodeToString((configuration.username + ":" + configuration.password).getBytes("UTF-8"));
            connection.setRequestProperty("Authorization", "Basic " + encoded);
            connection.setDoOutput(false);
            connection.setDoInput(true);
            connection.setConnectTimeout(configuration.timeout.toBigInteger().intValue() * 1000);
            connection.connect();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(connection.getInputStream());
            if (connection.getResponseCode() != 200 || doc == null) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error connecting to web interface - check network address / username / password");
                return;
            } else {
                updateStatus(ThingStatus.ONLINE);
            }
            connection.disconnect();

            /*
             * Example Response
             * <response>
             * <curfan>AUTO</curfan>
             * <curmode>OFF</curmode>
             * <curtemp>20</curtemp>
             * <outdoor>--</outdoor>
             * <outhum>--</outhum>
             * <humidity>50</humidity>
             * <sptcool>24</sptcool>
             * <sptheat>17</sptheat>
             * <ind0>0</ind0>
             * <ind1>0</ind1>
             * <ind2>0</ind2>
             * <cursched>Night</cursched>
             * <curday>Wednesday</curday>
             * <curtime>1:09 PM</curtime>
             * <sysstat>IDLE</sysstat>
             * <sysadapt> </sysadapt>
             * <schedstat1>Schedule</schedstat1>
             * <schedstat>Is Running (Wed-Night)</schedstat>
             * <ishumidity>1</ishumidity>
             * <ishumidityinternal>0</ishumidityinternal>
             * <sensor0>20</sensor0>
             * <sensor1>--</sensor1>
             * <sensor2>50</sensor2>
             * <sensor3>--</sensor3>
             * <sensor4>--</sensor4>
             * <sensor5>--</sensor5>
             * </response>
             */

            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_INDOOR_TEMP).getUID(),
                    new DecimalType(Double.parseDouble(nodeValue(doc, "curtemp"))));
            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_INDOOR_HUM).getUID(),
                    new DecimalType(Double.parseDouble(nodeValue(doc, "humidity"))));
            if (!nodeValue(doc, "outdoor").equals("--")) {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_OUTDOOR_TEMP).getUID(),
                        new DecimalType(Double.parseDouble(nodeValue(doc, "outdoor"))));
            } else {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_OUTDOOR_TEMP).getUID(),
                        new DecimalType(0));
            }
            if (!nodeValue(doc, "outhum").equals("--")) {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_OUTDOOR_HUM).getUID(),
                        new DecimalType(Double.parseDouble(nodeValue(doc, "outhum"))));
            } else {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_OUTDOOR_HUM).getUID(),
                        new DecimalType(0));
            }

            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_MODE).getUID(),
                    new StringType(nodeValue(doc, "curmode")));
            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_FAN_MODE).getUID(),
                    new StringType(nodeValue(doc, "curfan")));

            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_OVERRIDE).getUID(),
                    "Override".equals(nodeValue(doc, "schedstat1")) ? OnOffType.ON : OnOffType.OFF);

            // Active &#160;Min. Remaining: 479
            Matcher m = Pattern.compile(".*Remaining: (\\d+)").matcher(nodeValue(doc, "schedstat"));
            if (m.matches()) {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_OVERRIDE_TIMER).getUID(),
                        new DecimalType(Integer.parseInt(m.group(1))));
            } else {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_OVERRIDE_TIMER).getUID(),
                        new DecimalType(0));
            }

            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COOL_SETPOINT).getUID(),
                    new DecimalType(Double.parseDouble(nodeValue(doc, "sptcool"))));
            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_HEAT_SETPOINT).getUID(),
                    new DecimalType(Double.parseDouble(nodeValue(doc, "sptheat"))));

            String sysstat = nodeValue(doc, "sysstat");
            if (sysstat.equals("IDLE")) {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_MODE).getUID(),
                        new StringType("OFF"));
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_STAGE).getUID(),
                        new StringType("0"));
            } else if (sysstat.startsWith("HEAT")) {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_MODE).getUID(),
                        new StringType("HEAT"));
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_STAGE).getUID(),
                        new StringType(sysstat.substring(7, 8)));
            } else if (sysstat.startsWith("COOL")) {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_MODE).getUID(),
                        new StringType("COOL"));
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_STAGE).getUID(),
                        new StringType(sysstat.substring(7, 8)));
            } else {
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_MODE).getUID(),
                        null);
                updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_COMPRESSOR_STAGE).getUID(),
                        null);
            }
            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_LED1).getUID(),
                    "0".equals(nodeValue(doc, "ind0")) ? OnOffType.OFF : OnOffType.ON);
            updateState(getThing().getChannel(NetworkThermostatBindingConstants.CHANNEL_LED2).getUID(),
                    "0".equals(nodeValue(doc, "ind2")) ? OnOffType.OFF : OnOffType.ON);

        } catch (IOException | ParserConfigurationException | SAXException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error connecting to web interface - check network address / username / password: " + e.toString());
        }
    }

    private String nodeValue(Document doc, String nodeName) {
        return doc.getDocumentElement().getElementsByTagName(nodeName).item(0).getFirstChild().getNodeValue();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        /*
         * mode:heat
         * sp_cool:24
         * sp_heat:22
         * fan:on
         * update:Update
         *
         * mode:heat
         * sp_cool:24
         * sp_heat:22
         * fan:on
         * resume:Resume
         */
        try {
            if (channelUID.getId().equals(NetworkThermostatBindingConstants.CHANNEL_OVERRIDE)
                    && command.equals(OnOffType.OFF)) {
                logger.debug("Cancel override");
                postData("resume=Resume");
            } else if (channelUID.getId().equals(NetworkThermostatBindingConstants.CHANNEL_MODE)) {
                logger.debug("Update mode to " + command.toString().toLowerCase());
                postData("mode=" + command.toString().toLowerCase() + "&update=Update");
            } else if (channelUID.getId().equals(NetworkThermostatBindingConstants.CHANNEL_COOL_SETPOINT)) {
                logger.debug("Update cool setpoint to " + command.toString().toLowerCase());
                postData("sp_cool=" + command.toString() + "&update=Update");
            } else if (channelUID.getId().equals(NetworkThermostatBindingConstants.CHANNEL_HEAT_SETPOINT)) {
                logger.debug("Update heat setpoint to " + command.toString().toLowerCase());
                postData("sp_heat=" + command.toString() + "&update=Update");
            } else if (channelUID.getId().equals(NetworkThermostatBindingConstants.CHANNEL_FAN_MODE)) {
                logger.debug("Update fan mode to " + command.toString().toLowerCase());
                postData("fan=" + command.toString().toLowerCase() + "&update=Update");
            }

        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Error sending command "
                    + command.toString() + " to " + channelUID.toString() + " - " + e.toString());
        }

    }

    private void postData(String postData) throws IOException {
        if (configuration.readonly) {
            logger.debug("Request to send command " + postData + " to read-only thing " + getThing().getUID());
            return;
        }

        URL url = new URL("http:/" + inetAddress + "/index.htm");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        String encoded = Base64.getEncoder()
                .encodeToString((configuration.username + ":" + configuration.password).getBytes("UTF-8"));
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", Integer.toString(postData.length()));
        connection.setConnectTimeout(configuration.timeout.toBigInteger().intValue() * 1000);
        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        wr.write(postData);
        wr.flush();
        connection.connect();
        wr.close();
        connection.disconnect();
        if (connection.getResponseCode() != 200) {
            throw new IOException(
                    "Invalid response code " + connection.getResponseCode() + " " + connection.getResponseMessage());
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.INITIALIZING);
        logger.debug("Initializing Network Thermostat handler.");
        this.configuration = getConfigAs(NetworkThermostatConfiguration.class);
        logger.debug("Network Thermostat IP {}.", configuration.ipAddress);
        if (this.configuration.ipAddress == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Can not access device as ip address is invalid");
            logger.warn("Can not access device as ip address is invalid");
            return;
        } else
            if ("127.0.0.1".equals(this.configuration.ipAddress) || "localhost".equals(this.configuration.ipAddress)) {
            inetAddress = InetAddress.getLoopbackAddress();
        } else {
            try {
                inetAddress = InetAddress.getByName(this.configuration.ipAddress);
            } catch (UnknownHostException e1) {
                updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                        "IP Address not resolvable");
                return;
            }
        }
        // use default refresh interval if not specified
        int refreshInterval = DEFAULT_REFRESH_INTERVAL;
        if (this.configuration.refresh != null) {
            refreshInterval = this.configuration.refresh.toBigInteger().intValue();
        }
        logger.debug("Starting polling job with refresh interval of " + refreshInterval + " seconds");
        pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, (int) (refreshInterval * Math.random()),
                refreshInterval, TimeUnit.SECONDS);

    }

    @Override
    public void dispose() {
        logger.debug("Disposing Network Thermostat handler.");
        if (pollingJob != null) {
            pollingJob.cancel(true);
        }
    }

}