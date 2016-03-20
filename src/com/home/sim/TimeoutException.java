package com.home.sim;

/**
 * Created by Filip on 3/18/2016.
 */
public class TimeoutException extends Exception {
    private String toAddress;
    private String messageSent;

    public TimeoutException(String message, String toAddress, String messageSent) {
        super(message);
        this.toAddress = toAddress;
        this.messageSent = messageSent;
    }

    public String getToAddress() {
        return toAddress;
    }

    @Override
    public String toString() {
        return "TimeoutException{" +
                "toAddress='" + toAddress + '\'' +
                ", messageSent='" + messageSent + '\'' +
                '}';
    }

    public String getMessageSent() {
        return messageSent;
    }
}
