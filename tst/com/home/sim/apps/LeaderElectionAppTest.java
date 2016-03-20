package com.home.sim.apps;

import com.home.sim.Controller;
import com.home.sim.Host;
import com.home.sim.Network;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Filip on 3/19/2016.
 */


public class LeaderElectionAppTest {

    public static final String HOST_1 = "host/1";
    public static final String HOST_2 = "host/2";
    public static final String HOST_3 = "host/3";
    public static final String HOST_4 = "host/4";
    public static final String HOST_5 = "host/5";
    private List<Host> hosts = new ArrayList<>();
    private Network network;
    private Controller controller;


    @Before
    public void before() {
        BaseApp.logEnabled = false;

        network = new Network();

        Host host1 = new Host(HOST_1, () -> new LeaderElectionApp(), network);
        network.addHost(host1);
        hosts.add(host1);

        Host host2 = new Host(HOST_2, () -> new LeaderElectionApp(), network);
        network.addHost(host2);
        hosts.add(host2);

        Host host3 = new Host(HOST_3, () -> new LeaderElectionApp(), network);
        network.addHost(host3);
        hosts.add(host3);

        Host host4 = new Host(HOST_4, () -> new LeaderElectionApp(), network);
        network.addHost(host4);
        hosts.add(host4);


        Host host5 = new Host(HOST_5, () -> new LeaderElectionApp(), network);
        network.addHost(host5);
        hosts.add(host5);

        //network.isolateHost(host2);

        controller = new Controller(network);
    }

    @Test
    public void testHappyCase() throws InterruptedException {
        hosts.forEach(Host::start);
        hosts.forEach(host -> ((LeaderElectionApp) host.getApp()).setMinHeartbeat(Duration.of(200, ChronoUnit.MILLIS)));


        Thread.sleep(15000);

        int leadersCount = 0;

        LeaderElectionApp leader = null;

        for (Host host : hosts) {

            if(((LeaderElectionApp) host.getApp()).amITheLeader()){
                leader = ((LeaderElectionApp) host.getApp());
                leadersCount++;
            }

        }


        Assert.assertEquals("leaders found",1,leadersCount);

        System.out.println(leader.getCurrentElectionRound());
        Assert.assertTrue(leader.getCurrentElectionRound() < 10);

    }
}
