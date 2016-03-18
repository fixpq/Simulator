package com.home.sim;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * Created by Filip on 3/17/2016.
 */
public class Host {
    private final String address;
    private Thread thread;
    private final Network network;
    private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String, Message> replies = new ConcurrentHashMap<>();

    public String getAddress() {
        return address;
    }

    public Host(String address, Network network) {
        this.address = address;
        this.network = network;
    }

    public void handleCommand(String command) {
        if (command.startsWith("ping ")) {
            writeToConsole(command);
            String destination = command.split(" ")[1];
            Message reply = sendMessage(new Message(getAddress(), destination, "ping"));
            writeToConsole(reply);
        } else {
            writeToConsole("unknown command: " + command);
        }
    }


    public void receiveMessage(Message message) {
        if (message.isReply()) {
            replies.put(message.getId(), message);
        } else {
            messages.add(message);
        }
    }

    public Message sendMessage(Message message) {
        network.sentMessage(message);

        CompletableFuture<Message> futureReply = CompletableFuture.supplyAsync(() -> listenForReply(message.getId(), 60000));

        try {
            return futureReply.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("There should have been a reply!");
    }

    private Message listenForReply(String messageId, long maxTimeoutMilliseconds){
        Instant start = Instant.now();
        while(Duration.between(start,Instant.now()).toMillis() < maxTimeoutMilliseconds){
            Message reply = replies.remove(messageId);
            if(reply!=null){
                return reply;
            }
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return Message.getTimeoutReplay(messageId);
    }

    private Message handleMessage(Message message) {
        if (message.getContent().equals("ping")) {
            return message.getReturnMessage("pong");
        } else if (message.getContent().startsWith("pong")) {
            writeToConsole(message.getFrom() + " " + message.getContent());
            return null;
        } else {
            throw new NotImplementedException();
        }
    }

    public void start() {
        if (thread != null) {
            throw new IllegalStateException("Host already started");
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Message message = messages.poll();
                    if (message == null) {
                        continue;
                    }
                    Message response = handleMessage(message);
                    network.sentMessage(response);
                }
            }
        });
        thread.start();
    }

    public void writeToConsole(String message) {
        if (message != null && !message.equals("")) {
            System.out.printf("%s> %s%n", address, message);
        }
    }

    public void writeToConsole(Message message) {
        if (message != null) {
            System.out.printf("%s> %s%n", address, message.getContent());
        }
    }

    public Network getNetwork() {
        return network;
    }

    public ConcurrentLinkedQueue<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return address;
    }
}
