package com.home.sim.apps;

import com.home.sim.Host;
import com.home.sim.TimeoutException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Filip on 3/18/2016.
 */
public abstract class BaseApp implements Runnable {

    public static boolean logEnabled = true;

    private AtomicBoolean running = new AtomicBoolean(false);

    public static final String METHOD_NOT_FOUND = "METHOD NOT FOUND:";

    private Thread thread;

    private Host host;

    public void setHost(Host host) {
        this.host = host;
    }


    protected abstract String handleMessage(String message);


    public String handle(String message) {
        String response = handleMessage(message);
        log(message + " >> " + response);
        return response;
    }

    public void stop() {
        running.compareAndSet(true, false);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        if (thread != null) {
            throw new IllegalStateException("Host already started");
        }

        thread = new Thread(this);
        thread.start();
    }

    protected String sendMessage(String toAddress, String content) throws Exception {
        return host.sendMessage(toAddress, content);
    }

    protected void println(String line) {
        host.writeToConsole(line);
    }

    protected void println(Exception e) {
        try {
            throw e;
        } catch (TimeoutException e2) {
            //host.writeToConsole(e2.toString());
        } catch (Exception e1) {
            host.writeToConsole("[ERR] " + e1.getMessage());
            e1.printStackTrace();
        }
    }

    protected void log(String line) {
        if (logEnabled)
            host.writeToConsole("[DEBUG]" + line);
    }

    protected String getIp() {
        return host.getAddress();
    }

    public abstract String getState();
}
