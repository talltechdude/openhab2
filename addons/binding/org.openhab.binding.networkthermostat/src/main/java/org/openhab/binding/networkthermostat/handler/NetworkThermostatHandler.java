/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.networkthermostat.handler;

import static org.openhab.binding.networkthermostat.NetworkThermostatBindingConstants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.networkthermostat.config.NetworkThermostatConfiguration;
import org.openhab.binding.networkthermostat.internal.FanModeType;
import org.openhab.binding.networkthermostat.internal.ThermostatModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NetworkThermostatHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Linton - Initial contribution
 */
public class NetworkThermostatHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Socket socket;
    private InetAddress ipAddress;
    private int port = 10001;
    private BufferedReader in;
    private PrintWriter out;

    private static final int DEFAULT_REFRESH_INTERVAL = 60;
    private ScheduledFuture<?> pollingJob;

    private ExecutorService threadPool = ThreadPoolManager.getPool(this.getClass().getSimpleName());

    private Runnable pollingRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                updateThermostatStatus();
            } catch (Exception e) {
                logger.debug("Exception during poll: {}", e);
            }
        }
    };

    private Runnable thermostatReader = new Thread() {

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    if (socket == null || !socket.isConnected() || socket.isClosed()) {
                        connect();
                    }
                    String line = in.readLine();
                    if (line != null) {
                        processResponse(line);
                        continue;
                    } else {
                        close();
                    }
                } catch (Exception e) {

                } finally {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            }
        }
    };

    public NetworkThermostatHandler(Thing thing) {
        super(thing);
    }

    private void processResponse(String response) {
        try {
            logger.debug("Response received from thermostat {}: {}", getThing().getUID(), response);
            int sep = response.indexOf(":");
            String command = response.substring(0, sep);
            String data = response.substring(sep + 1);
            if (command.equals("RAS1")) {
                String[] values = data.split(",");
                // Indoor Temp, Outdoor Temp, Mode, Fan, Override, Recovery, Cool Setpoint, Heat Setpoint, Status,
                // Stages
                updateState(getThing().getChannel(CHANNEL_INDOOR_TEMP).getUID(),
                        new DecimalType(Double.parseDouble(values[0])));
                if (!values[1].equals("NA")) {
                    updateState(getThing().getChannel(CHANNEL_OUTDOOR_TEMP).getUID(),
                            new DecimalType(Double.parseDouble(values[1])));
                } else {
                    updateState(getThing().getChannel(CHANNEL_OUTDOOR_TEMP).getUID(), new DecimalType(0));
                }
                updateState(getThing().getChannel(CHANNEL_MODE).getUID(), new StringType(values[2]));
                updateState(getThing().getChannel(CHANNEL_FAN_MODE).getUID(), new StringType(values[3].substring(4)));
                updateState(getThing().getChannel(CHANNEL_OVERRIDE).getUID(),
                        "YES".equals(values[4]) ? OnOffType.ON : OnOffType.OFF);
                updateState(getThing().getChannel(CHANNEL_COOL_SETPOINT).getUID(),
                        new DecimalType(Double.parseDouble(values[6])));
                updateState(getThing().getChannel(CHANNEL_HEAT_SETPOINT).getUID(),
                        new DecimalType(Double.parseDouble(values[7])));
                updateState(getThing().getChannel(CHANNEL_COMPRESSOR_MODE).getUID(), new StringType(values[8]));
                updateState(getThing().getChannel(CHANNEL_COMPRESSOR_STAGE).getUID(), new StringType(values[9]));
            } else if (command.equals("RAOS1")) {
                // RAOS1:15-10-15,17:27:53,0:00,0:0:00,TH3,DISABLED
                String[] values = data.split(",");
                String override[] = values[2].split(":");
                updateState(getThing().getChannel(CHANNEL_OVERRIDE_TIMER).getUID(),
                        new DecimalType(Integer.parseInt(override[0]) * 60 + Integer.parseInt(override[1])));
            } else if (command.equals("RAAS1")) {
                // RAAS1:YES,NO,NO,NO,NO,YES,NO,NO,NO,NO,NO,YES,NO,NO,NO,NO
                String[] values = data.split(",");
                updateState(getThing().getChannel(CHANNEL_LED1).getUID(),
                        "YES".equals(values[14]) ? OnOffType.ON : OnOffType.OFF);
                updateState(getThing().getChannel(CHANNEL_LED2).getUID(),
                        "YES".equals(values[15]) ? OnOffType.ON : OnOffType.OFF);
            }
        } catch (Exception e) {
            logger.error("Failed to parse response: {} - {}", response, e.getMessage());
        }
    }

    private void updateThermostatStatus() {
        if (getThing().getStatus().equals(ThingStatus.ONLINE)) {
            sendCommand("RAS1");
            sendCommand("RAOS1");
            sendCommand("RAAS1");
        }
    }

    // protected void updateThermostatStatus2() {
    // String result = sendCommand("RAS1");
    // logger.trace("RAS1: {}", result);
    // // Indoor Temp, Outdoor Temp, Mode, Fan, Override, Recovery, Cool Setpoint, Heat Setpoint, Status, Stages
    // String[] values = result.substring(5).split(",");
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_INDOOR_TEMP),
    // new DecimalType(Double.parseDouble(values[0])));
    // if (!values[1].equals("NA")) {
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_OUTDOOR_TEMP),
    // new DecimalType(Double.parseDouble(values[1])));
    // } else {
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_OUTDOOR_TEMP), new DecimalType(0));
    // }
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_MODE), new StringType(values[2]));
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_FAN_MODE), new StringType(values[3].substring(4)));
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_OVERRIDE),
    // "YES".equals(values[4]) ? OnOffType.ON : OnOffType.OFF);
    //
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_COOL_SETPOINT),
    // new DecimalType(Double.parseDouble(values[6])));
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_HEAT_SETPOINT),
    // new DecimalType(Double.parseDouble(values[7])));
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_COMPRESSOR_MODE), new StringType(values[8]));
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_COMPRESSOR_STAGE), new StringType(values[9]));
    //
    // result = sendCommand("RAOS1"); // RAOS1:15-10-15,17:27:53,0:00,0:0:00,TH3,DISABLED
    // logger.trace("RAOS1: {}", result);
    // values = result.substring(6).split(",");
    // String override[] = values[2].split(":");
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_OVERRIDE_TIMER),
    // new DecimalType(Integer.parseInt(override[0]) * 60 + Integer.parseInt(override[1])));
    //
    // result = sendCommand("RAAS1"); // RAAS1:YES,NO,NO,NO,NO,YES,NO,NO,NO,NO,NO,YES,NO,NO,NO,NO
    // logger.trace("RAAS1: {}", result);
    // values = result.substring(6).split(",");
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_LED1),
    // "YES".equals(values[14]) ? OnOffType.ON : OnOffType.OFF);
    // updateState(new ChannelUID(getThing().getUID(), CHANNEL_LED2),
    // "YES".equals(values[15]) ? OnOffType.ON : OnOffType.OFF);
    //
    // /*
    // * <channel id="cool_setpoint" typeId="setpoint"/>
    // * <channel id="heat_setpoint" typeId="setpoint"/>
    // * <channel id="mode" typeId="mode"/>
    // * <channel id="indoor_temp" typeId="current_temp"/>
    // * <channel id="outdoor_temp" typeId="current_temp"/>
    // * <channel id="compressor_mode" typeId="compressor_mode"/>
    // * <channel id="compressor_stage" typeId="compressor_stage"/>
    // */
    //
    // // TODO Auto-generated method stub
    //
    // }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_MODE)) {
            ThermostatModeType modeCommand = ThermostatModeType.valueOf(command.toString());
            switch (modeCommand) {
                case AUTO:
                    logger.info("Updating mode: {}", sendCommand("WMS1DA"));
                    break;
                case COOL:
                    logger.info("Updating mode: {}", sendCommand("WMS1DC"));
                    break;
                case EHEAT:
                    logger.info("Updating mode: {}", sendCommand("WMS1DE"));
                    break;
                case HEAT:
                    logger.info("Updating mode: {}", sendCommand("WMS1DH"));
                    break;
                case OFF:
                    logger.info("Updating mode: {}", sendCommand("WMS1DO"));
            }
            updateThermostatStatus();
        } else if (channelUID.getId().equals(CHANNEL_FAN_MODE)) {
            FanModeType modeCommand = FanModeType.valueOf(command.toString());
            switch (modeCommand) {
                case AUTO:
                    sendCommand("WFM1DA");
                    break;
                case ON:
                    sendCommand("WFM1DO");
                    break;
                case RECIRC:
                    sendCommand("WMS1DR");
            }
            updateThermostatStatus();
        } else if (channelUID.getId().equals(CHANNEL_COOL_SETPOINT)) {
            logger.info("Override cool setpoint to {}: {}", command.toString(),
                    sendCommand("WOS1DX,X," + command.toString() + ",X"));
            updateThermostatStatus();
        } else if (channelUID.getId().equals(CHANNEL_HEAT_SETPOINT)) {
            logger.info("Override heat setpoint to {}: {}", command.toString(),
                    sendCommand("WOS1DX,X,X," + command.toString()));
            updateThermostatStatus();
        } else if (channelUID.getId().equals(CHANNEL_OVERRIDE) && command.equals(OnOffType.OFF)) {
            logger.info("Cancel override: {}", sendCommand("WOR1D0:00"));
            updateThermostatStatus();

        } else if (channelUID.getId().equals(CHANNEL_OVERRIDE_TIMER)) {
            double timer = Double.parseDouble(command.toString());
            int hours = (int) Math.floor(timer / 60);
            int mins = (int) (timer % 60);
            logger.info("Cancel override: {}",
                    sendCommand("WOR1D" + Integer.toString(hours) + ":" + String.format("%02d", mins)));
            updateThermostatStatus();
        }

    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.INITIALIZING);
        logger.debug("Initializing Network Thermostat handler.");
        NetworkThermostatConfiguration configuration = getConfigAs(NetworkThermostatConfiguration.class);
        logger.debug("Network Thermostat IP {}.", configuration.ipAddress);
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
        super.initialize();
        updateStatus(ThingStatus.INITIALIZING);
        onUpdate();
        threadPool.execute(thermostatReader);
    }

    private void connect() {
        try {
            socket = new Socket(ipAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            logger.info("Logging in...");
            sendCommand("WML1Dadmin,netx");
            updateStatus(ThingStatus.ONLINE);
        } catch (IOException e) {
            logger.debug("Failed to connect to thermostat {} at {}:{}", getThing().getUID(), ipAddress, port);
            close();
        }
    }

    private void close() {
        try {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            socket.close();
        } catch (IOException e) {
            logger.debug("Failed to disconnect to thermostat {} at {}:{}", getThing().getUID(), ipAddress, port);
        }
    }

    private boolean sendCommand(String command) {
        if (out == null) {
            return false;
        }
        logger.debug("Sending command to thermostat {}: {}", getThing().getUID(), command);
        out.println(command);
        return true;
    }

    private synchronized void onUpdate() {
        if (pollingJob == null || pollingJob.isCancelled()) {
            Configuration config = getThing().getConfiguration();
            // use default if not specified
            int refreshInterval = DEFAULT_REFRESH_INTERVAL;
            Object refreshConfig = config.get(NetworkThermostatConfiguration.REFRESH);
            if (refreshConfig != null) {
                refreshInterval = ((BigDecimal) refreshConfig).intValue();
            }
            pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, (int) (refreshInterval * Math.random()),
                    refreshInterval, TimeUnit.SECONDS);
        }
    }
}