package com.home.sim;

import com.home.sim.apps.HeartbeatApp;

public class Main {

    public static final String HOST_1 = "host/1";
    public static final String HOST_2 = "host/2";
    public static final String HOST_3 = "master";

    public static void main(String[] args) {
        Network network = new Network();

        Host host1 = new Host(HOST_1, new HeartbeatApp(), network);
        host1.start();
        network.addHost(host1);

        Host host2 = new Host(HOST_2, new HeartbeatApp(), network);
        host2.start();
        network.addHost(host2);

        Host host3 = new Host(HOST_3, new HeartbeatApp(), network);
        host3.start();
        network.addHost(host3);

        network.isolateHost(host2);

        Controller controller = new Controller(network);

        controller.start();

    }
}
