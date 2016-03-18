package com.home.sim;

import com.home.sim.apps.BaseApp;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * Created by Filip on 3/17/2016.
 */
public class Host {
    private final String address;
    private final BaseApp app;
    private Thread thread;
    private final Network network;
    private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String, Message> replies = new ConcurrentHashMap<>();

    public String getAddress() {
        return address;
    }


    public Host(String address, BaseApp app, Network network) {
        this.address = address;
        this.app = app;
        this.app.setHost(this);
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

    public String sendMessage(String toAddress, String content) throws Exception {
        Message message = new Message(getAddress(), toAddress, content);
        Message reply = sendMessage(message);
        if (reply.isException()) {
            if (reply.isTimeout()) {
                throw new TimeoutException(reply.getContent(), toAddress, content);
            }
            throw new Exception("Exception returned: " + reply.getContent());
        }
        return reply.getContent();
    }

    public Message sendMessage(Message message) {
        network.sentMessage(message);

        CompletableFuture<Message> futureReply = CompletableFuture.supplyAsync(() -> listenForReply(message.getId(), 2000));

        try {
            return futureReply.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("There should have been a reply!");
    }

    private Message listenForReply(String messageId, long maxTimeoutMilliseconds) {
        Instant start = Instant.now();
        while (Duration.between(start, Instant.now()).toMillis() < maxTimeoutMilliseconds) {
            Message reply = replies.remove(messageId);
            if (reply != null) {
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
            String response = app.handle(message.getContent());
            if (BaseApp.METHOD_NOT_FOUND.equals(response)) {
                return message.getReturnException(BaseApp.METHOD_NOT_FOUND);
            } else {
                return message.getReturnMessage(response);
            }

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
        this.app.start();
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

    public BaseApp getApp() {
        return app;
    }
}
