package org.openhab.binding.cbus.internal.cgate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public abstract class CGateSocket {

    private Socket socket;
    private InetAddress ipAddress;
    private int port;
    private BufferedReader in;
    private PrintWriter out;
    private Thread monitorThread;
    private boolean shutdownRequested;

    public CGateSocket(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public void connect() {
        shutdownRequested = false;
        monitorThread = new Thread(socketMonitor);
        monitorThread.setName("CGateSocketThread");
        monitorThread.start();
    }

    private Runnable socketMonitor = new Runnable() {

        @Override
        public void run() {
            while (true) {
                try {
                    if (shutdownRequested) {
                        if (socket != null) {
                            socket.close();
                        }
                        break;
                    }
                    if (!isConnected()) {
                        socket = new Socket(ipAddress, port);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out = new PrintWriter(socket.getOutputStream(), true);
                    }
                    String line = in.readLine();
                    parseLine(line);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        }

    };

    public void disconnect() {
        shutdownRequested = true;
    }

    public void println(String line) {
        if (isConnected()) {
            out.println(line);
        }
    }

    public abstract void parseLine(String line);

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
