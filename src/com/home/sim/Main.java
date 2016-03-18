package com.home.sim;

import com.home.sim.apps.HeartbeatApp;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static final String HOST_1 = "host/1";
    public static final String HOST_2 = "host/2";
    public static final String HOST_3 = "host/3";
    public static final String HOST_4 = "host/4";
    public static final String HOST_5 = "host/5";

    private static Network network = new Network();

    public static void main(String[] args) {
        Host host1 = new Host(HOST_1, new HeartbeatApp(), network);
        host1.start();
        network.addHost(host1);

        Host host2 = new Host(HOST_2, new HeartbeatApp(), network);
        host2.start();
        network.addHost(host2);

        Host host3 = new Host(HOST_3, new HeartbeatApp(), network);
        host3.start();
        network.addHost(host3);

        Host host4 = new Host(HOST_4, new HeartbeatApp(), network);
        host4.start();
        network.addHost(host4);

        Host host5 = new Host(HOST_5, new HeartbeatApp(), network);
        host5.start();
        network.addHost(host5);

        //network.isolateHost(host2);

        Controller controller = new Controller(network);

        controller.start();

        network.getAllHosts().forEach(host -> System.out.println(host.getApp().getState()));

        System.exit(0);

    }
}
