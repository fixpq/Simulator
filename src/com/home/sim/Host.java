package com.home.sim;

import com.home.sim.apps.BaseApp;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Created by Filip on 3/17/2016.
 */
public class Host {
    private final String address;
    //the app running on this host
    private BaseApp app;
    private final Supplier<BaseApp> appFactory;
    private int networkCommunicationTimeoutMs = 2000;

    private AtomicBoolean running = new AtomicBoolean(false);

    private Thread thread;
    private final Network network;

    private ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String, Message> replies = new ConcurrentHashMap<>();

    public String getAddress() {
        return address;
    }


    public Host(String address, Supplier<BaseApp> appFactory, Network network) {
        this.address = address;
        this.appFactory = appFactory;
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

        CompletableFuture<Message> futureReply = CompletableFuture.supplyAsync(() -> listenForReply(message.getId(),
                getNetworkCommunicationTimeoutMs()));

        try {
            return futureReply.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("There should have been a reply!");
    }

    public int getNetworkCommunicationTimeoutMs() {
        return networkCommunicationTimeoutMs;
    }

    public void setNetworkCommunicationTimeoutMs(int value){
        this.networkCommunicationTimeoutMs = value;
    }

    private Message listenForReply(String messageId, long maxTimeoutMilliseconds) {
        Instant start = Instant.now();
        while (Duration.between(start, Instant.now()).toMillis() < maxTimeoutMilliseconds) {
            Message reply = replies.remove(messageId);
            if (reply != null) {
                return reply;
            }
            try {
                Thread.sleep(2L);
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
            if (response.startsWith(BaseApp.METHOD_NOT_FOUND)) {
                return message.getReturnException(response);
            } else {
                return message.getReturnMessage(response);
            }

        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            app.stop();
            app = null;
            this.messages.clear();
            this.replies.clear();
        }

    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        if (thread != null) {
            throw new IllegalStateException("Host already started");
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
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
        this.app = this.appFactory.get();
        this.app.setHost(this);
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
