package org.openhab.binding.cbus.internal.cgate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

public class CGateCommandSocket extends CGateSocket {
    private Logger logger = LoggerFactory.getLogger(CGateCommandSocket.class);

    private int commandID = 0;
    private final int commandIDPrefix = this.hashCode();
    private final Map<String, BufferedWriter> responses = Collections
            .synchronizedMap(new HashMap<String, BufferedWriter>());
    private final Map<String, CommandStatus> status = Collections.synchronizedMap(new HashMap<String, CommandStatus>());

    private enum CommandStatus {
        SENDING,
        PROCESSING,
        COMPLETE
    }

    public CGateCommandSocket(InetAddress ipAddress, int port) {
        super(ipAddress, port);
    }

    @Override
    public void parseLine(String line) {
        logger.info("Line received: {}", line);
        if (line == null)
            return;
        if (line.charAt(0) == '[') {
            // Response to command
            String id = line.substring(1, line.indexOf(']'));
            String response = line.substring(line.indexOf(']') + 2);
            try {
                responses.get(id).write(response, 0, response.length());
                responses.get(id).newLine();

                if (!response.substring(3, 4).equals("-")) {
                    status.put(id, CommandStatus.COMPLETE);
                    responses.get(id).close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // int returnCode = Integer.parseInt(line.substring(0, 3));
        // switch (returnCode) {
        // case 201:
        // break;
        // }

    }

    private final Pattern re_tagName = Pattern.compile("Network\\[(\\d+)\\]\\/TagName=(.*)");
    private final Pattern re_address = Pattern.compile("Network\\[(\\d+)\\]\\/Address=(.*)");

    public Map<Integer, String> listNetworks() {
        Map<Integer, String> result = new HashMap<Integer, String>();
        String project = Joiner.on('\n').join(sendCommand("dbget Project/Network").toArray());
        for (int i = 1; true; i++) {
            Matcher mAddress = Pattern.compile("Network\\[" + i + "\\]/Address=(.*)").matcher(project);
            Matcher mTag = Pattern.compile("Network\\[" + i + "\\]/TagName=(.*)").matcher(project);
            if (mAddress.find() && mTag.find()) {
                result.put(Integer.parseInt(mAddress.group(1)), mTag.group(1));
            } else {
                break;
            }
        }
        /*
         * for (String line : tmp) {
         * Matcher m = re_tagName.matcher(line);
         * if (m.find()) {
         * logger.info("regex: {}, {}", m.group(1), m.group(2));
         * tagName.put(Integer.parseInt(m.group(1)), m.group(2));
         * // m.group(1)
         * } else {
         * m = re_address.matcher(line);
         * if (m.find()) {
         * logger.info("regex: {}, {}", m.group(1), m.group(2));
         * address.put(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
         * }
         * }
         * }
         * for (Integer key : tagName.keySet()) {
         * result.put(address.get(key), tagName.get(key));
         * }
         */
        return result;
    }

    public ArrayList<String> sendCommand(String command) {
        ArrayList<String> result = new ArrayList<String>();
        String id = String.valueOf(commandIDPrefix + commandID++);
        BufferedReader response = getReader(id);
        status.put(id, CommandStatus.PROCESSING);
        println("[" + id + "] " + command);

        while (status.get(id).equals(CommandStatus.PROCESSING)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (status.get(id) == CommandStatus.COMPLETE) {
            String line;
            try {
                while ((line = response.readLine()) != null) {
                    result.add(line);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                result.add("-2");
            }
        }
        return result;
    }

    private BufferedReader getReader(String id) {
        try {
            PipedWriter piped_writer = new PipedWriter();
            BufferedWriter out = new BufferedWriter(piped_writer);
            responses.put(id, out);

            PipedReader piped_reader = new PipedReader(piped_writer);
            return new BufferedReader(piped_reader);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean isNetworkOnline(int network) {
        String result = Joiner.on('\n').join(sendCommand("GETSTATE " + network).toArray());
        return "200 OK.".equals(result);
    }

    private final Pattern re_tag = Pattern.compile("(\\d+)\\/(.+?)\\/(\\d+)\\/TagName=(.*)");

    public Map<Integer, String> listGroups(int networkID, int applicationID) {
        Map<Integer, String> result = new HashMap<Integer, String>();
        ArrayList<String> taglist = sendCommand("dbtaglist");
        for (String tag : taglist) {
            Matcher mTag = re_tag.matcher(tag);
            if (mTag.find()) {
                if (Integer.toString(networkID).equals(mTag.group(1))
                        && Integer.toString(applicationID).equals(mTag.group(2))) {
                    result.put(Integer.parseInt(mTag.group(3)), mTag.group(4));
                }
            } else {
                break;
            }
        }
        return result;
    }
}
