package com.home.sim;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Created by Filip on 3/17/2016.
 */
public class Message {
    private final String from;
    private final String to;
    private final String content;
    private String id = UUID.randomUUID().toString();
    private boolean isReply=false;

    private Instant time = Instant.now();
    private boolean isException = false;
    private boolean isTimeout = false;


    public static Message getTimeoutReplay(String id){
        Message reply = new Message("", "", "timeout");
        reply.setId(id);
        reply.setReply(true);
        reply.setTimeout(true);
        reply.setException(true);
        return reply;
    }

    public Message(String from, String to, String content) {
        this.from = from;
        this.to = to;
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getContent() {
        return content;
    }

    public Duration getAge(){
        return Duration.between(time,Instant.now());
    }

    private void setReply(boolean reply) {
        isReply = reply;
    }

    private void setId(String id){
        this.id = id;
    }

    public boolean isReply() {
        return isReply;
    }

    public String getId() {
        return id;
    }

    public boolean isException() {
        return isException;
    }

    private void setException(boolean exception) {
        isException = exception;
    }

    public boolean isTimeout() {
        return isTimeout;
    }

    private void setTimeout(boolean timeout) {
        isTimeout = timeout;
    }

    public Message getReturnMessage(String content){
        Message reply = new Message(getTo(), getFrom(), content);
        reply.setReply(true);
        reply.setId(this.getId());
        return reply;
    }

    public Message getReturnException(String content){
        Message reply = new Message(getTo(), getFrom(), content);
        reply.setReply(true);
        reply.setId(this.getId());
        reply.setException(true);
        reply.setException(true);
        return reply;
    }

    @Override
    public String toString() {
        return "Message{" +
                "content='" + content + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                '}';
    }
}
