package com.home.sim;

import java.util.ArrayList;
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

    //used to simulate a network split
    private HashMap<String, Host> isolatedHost = new HashMap<>();

    public void addHost(Host host){
        hosts.put(host.getAddress(),host);
    }

    public void isolateHost(Host host){
        isolatedHost.put(host.getAddress(),host);
    }

    public void remoteFromIsolation(Host host){
        isolatedHost.remove(host.getAddress());
    }

    public Host getHost(String address){
        return hosts.get(address);
    }

    public List<Host> getAllHosts(){
        return new ArrayList<>(hosts.values());
    }


    public void sentMessage(Message message){

        if(message == null){
            return;
        }

        Host source = hosts.get(message.getFrom());
        Host dest =  hosts.get(message.getTo());

        if(dest==null){
            sentMessage(message.getReturnException("destination not found: " + message.getTo()));
            return ;
        }

        if(isolatedHost.containsKey(message.getFrom()) || isolatedHost.containsKey(message.getTo())){
            return;
        }

        executor.submit(() -> {
            try {
                //simulate 0.1% failure rate
                boolean shouldFail = Math.abs(random.nextInt() % 1000) >= 999;
                if(shouldFail){
                    return;
                }
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
