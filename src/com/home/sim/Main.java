package com.home.sim;

public class Main {

    public static final String HOST_1 = "host/1";
    public static final String HOST_2 = "host/2";

    public static void main(String[] args) {
        Network network = new Network();

        Host host1 = new Host(HOST_1, network);
        host1.start();
        network.addHost(host1);

        Host host2 = new Host(HOST_2, network);
        host2.start();
        network.addHost(host2);

        Controller controller = new Controller(network);

        controller.start();

    }
}
