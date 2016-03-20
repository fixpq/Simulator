package com.home.sim.apps;

import com.home.sim.TimeoutException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;

import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Created by Filip on 3/17/2016.
 */
public class LeaderElectionApp extends BaseApp {

    private Random random = new Random();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private Duration minHeartbeat = Duration.of(10, ChronoUnit.SECONDS);
    private Duration timeout = Duration.of(Math.max(minHeartbeat.toMillis() * 2,
            Math.abs(random.nextInt() % minHeartbeat.toMillis() * 4)), MILLIS);
    private String currentLeader = null;
    private int currentElectionRound = 1;
    private ScheduledFuture<?> timeoutChecker;
    private boolean candidate = false;

    public LeaderElectionApp() {
    }

    protected String handleMessage(String message) {
        if (message.startsWith("heartbeat")) {
            String sender = message.split(" ")[1];
            String value = message.split(" ")[2];
            int electionRound = Integer.parseInt(value);
            if (electionRound < currentElectionRound) {
                return "not ok";
            } else {
                currentElectionRound = Integer.parseInt(value);
                currentLeader = sender;
                rescheduleTimeoutChecker();
                return "ok";
            }
        } else if (message.startsWith("requestVote")) {
            if (candidate) {
                return "no";
            }
            String sender = message.split(" ")[1];
            String value = message.split(" ")[2];
            int electionRound = Integer.parseInt(value);
            if (electionRound > currentElectionRound) {
                currentLeader = sender;
                currentElectionRound = electionRound;
                return "yes";
            } else if (electionRound < currentElectionRound) {
                return "no";
            } else if (electionRound == currentElectionRound) {
                if (!sender.equals(currentLeader)) {
                    return "no";
                } else {
                    return "yes";
                }
            }
        }

        return METHOD_NOT_FOUND + message;
    }

    public void sendHeartbeat(String to) {
        try {
            //simulate sending a heartbeat to a pre-configured machine
            String content = "heartbeat " + getIp() + " " + currentElectionRound;
            sendMessage(to, content);
        } catch (Exception e) {
            println(e);
        }
    }

    private ConcurrentSkipListSet<String> hostsInCluster = new ConcurrentSkipListSet<>();

    private void discoverHostsInCluster() {

        for (int i = 0; i < 10; i++) {
            String hostName = "host/" + i;
            if (hostName.equals(getIp())) {
                continue;
            }

            try {
                String response = sendMessage(hostName, "ping");
                if (response.startsWith("pong")) {
                    hostsInCluster.add(hostName);
                }
            } catch (TimeoutException e) {
//                log(e.toString());
            } catch (Exception e) {
                hostsInCluster.remove(hostName);
                // println(e.getMessage());
            }
        }
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public int getCurrentElectionRound() {
        return currentElectionRound;
    }

    public ScheduledFuture<?> getTimeoutChecker() {
        return timeoutChecker;
    }

    public boolean isCandidate() {
        return candidate;
    }

    public void setMinHeartbeat(Duration minHeartbeat) {
        this.minHeartbeat = minHeartbeat;
        this.timeout = Duration.of(minHeartbeat.toMillis() * 2 + Math.abs(random.nextInt() % minHeartbeat.toMillis()), MILLIS);
    }


    public void setTimeoutChecker(ScheduledFuture<?> timeoutChecker) {
        this.timeoutChecker = timeoutChecker;
    }

    public void setCandidate(boolean candidate) {
        this.candidate = candidate;
    }

    public void setCurrentElectionRound(int currentElectionRound) {
        this.currentElectionRound = currentElectionRound;
    }

    public void setCurrentLeader(String currentLeader) {
        this.currentLeader = currentLeader;
    }

    @Override
    public String getState() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("State for host: " + getIp() + "\n");
        stringBuilder.append("hostsInCluster:" + "\n");
        hostsInCluster.forEach(s -> stringBuilder.append(s + ","));
        stringBuilder.append("---\n");
        stringBuilder.append("timeoutMs: " + timeout.toMillis() + "\n");
        stringBuilder.append("currentLeader: " + currentLeader + "\n");
        stringBuilder.append("currentElectionRound: " + currentElectionRound + "\n");
        return stringBuilder.toString();
    }

    private void timedOut() {
        try {
            startElection();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            candidate = false;
        }
        rescheduleTimeoutChecker();
    }

    private void startElection() {
        candidate = true;
//        log("starting elections");
        currentLeader = null;
        //I vote for myself
        //required 50% + 1
        int votesNecessary = ((hostsInCluster.size()) / 2) + 1;

        log("votesNecessary: " + votesNecessary);

        Hashtable<String, Optional<Boolean>> votesForMe = new Hashtable<>();

        hostsInCluster.forEach(s -> votesForMe.put(s, Optional.empty()));

        currentElectionRound++;

        hostsInCluster.forEach(host -> {
            try {
//                log("sending message to " + host);
                String requireVoteResponse = sendMessage(host, "requestVote " + getIp() + " " + currentElectionRound);
//                log("response from host " + host + " is  " + requireVoteResponse);
                if (requireVoteResponse.equals("yes")) {
                    votesForMe.put(host, Optional.of(true));
                } else {
                    votesForMe.put(host, Optional.of(false));
                }
            } catch (Exception e) {
                log(e.getMessage());
            }
        });


        int votes = 0;
        for (Map.Entry<String, Optional<Boolean>> entry : votesForMe.entrySet()) {
            if (entry.getValue().orElse(false)) {
                votes++;
            }
        }

        if (votes >= votesNecessary) {
            //I am the leader
            currentLeader = getIp();
            log("I am the leader!");
        } else {
            log("I lost the elections. Resseting local currentElectionRound to: " + --currentElectionRound);
            //hung elections or elections lost
            //TODO: should contact hosts which did not voted (the ones with votesForMe.get(host) == null
            rescheduleTimeoutChecker();
        }

        candidate = false;
    }

    public boolean amITheLeader() {
        return getIp().equals(currentLeader);
    }

    private void rescheduleTimeoutChecker() {
        if (amITheLeader()) {
            if (timeoutChecker != null)
                timeoutChecker.cancel(true);
            timeoutChecker = null;

        } else if (timeoutChecker == null) {
            timeoutChecker = scheduler.schedule(() -> {
                timedOut();
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        } else {

            timeoutChecker.cancel(true);
            timeoutChecker = scheduler.schedule(() -> {
                timedOut();
            }, timeout.toMillis() / 2 + 2, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void run() {
        scheduler.scheduleAtFixedRate(() -> {
            discoverHostsInCluster();
        }, 0, 30, TimeUnit.SECONDS);

        rescheduleTimeoutChecker();

        //if I am the leader, send a heartbeat to everyone else
        scheduler.scheduleAtFixedRate(() -> {
            sendHeartbeatIfLeader();
        }, 0, minHeartbeat.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeatIfLeader() {
        if (amITheLeader()) {
            hostsInCluster.forEach(host -> {
                        CompletableFuture.runAsync(() -> {
                            sendHeartbeat(host);
                        });
                    }
            );
        }
    }
}
