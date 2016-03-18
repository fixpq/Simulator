package com.home.sim;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by Filip on 3/17/2016.
 */
public class Network {
    private final Random random = new Random();

    private ExecutorService executor = Executors.newCachedThreadPool();

    private HashMap<String, Host> hosts = new HashMap<>();

    public void addHost(Host host){
        hosts.put(host.getAddress(),host);
    }

    public Host getHost(String address){
        return hosts.get(address);
    }


    public void sentMessage(Message message){

        if(message == null){
            return;
        }
        Host dest =  hosts.get(message.getTo());
        if(dest==null){
            sentMessage(message.getReturnException("destination not found: " + message.getTo()));
            return ;
        }

        executor.submit(() -> {
            try {
                //simulate network latency
                int millis = Math.max(Math.abs(random.nextInt() % 20),2);
                Thread.sleep(millis);
                dest.receiveMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
