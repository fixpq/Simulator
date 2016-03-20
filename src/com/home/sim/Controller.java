package com.home.sim;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Filip on 3/17/2016.
 */
public class Controller {
    private final Network network;

    private Host connectedHost = null;

    public Controller(Network network) {
        this.network = network;
    }

    public void startWithConsoleInput(){
        Scanner input = new Scanner(System.in);
        System.out.println(">");
        String command = input.nextLine();

        while (!command.equals("quit")) {
            try {
                System.out.println(executeCommand(command));
            } catch (Exception e) {
                System.out.println(e);
            }
            System.out.printf("%s>%n", connectedHost);
            command = input.nextLine();
        }

        System.out.println("---");
    }

    public String executeCommand(String command){
        //connect address
        if (command.startsWith("connect")) {
            String address = command.split(" ")[1];
            Host host = network.getHost(address);
            if(host == null){
                return "host " + address + " not found";
            }
            else {
                this.connectedHost = host;
                return "connected to " + address;
            }
        }
        else if(command.startsWith("iso")){
            String address = command.split(" ")[1];
            Host host = network.getHost(address);
            if(host == null){
                return "host " + address + " not found";
            }
            else {
                network.isolateHost(host);
                return "isolating host " + address;
            }
        }
        else if(command.startsWith("uniso")){
            String address = command.split(" ")[1];
            Host host = network.getHost(address);
            if(host == null){
                return "host " + address + " not found";
            }
            else {
                network.remoteFromIsolation(host);
                return "unisolating host " + address;
            }
        }
        else if(command.startsWith("ping")){
            sendCommandToConnetedHost(command);
        }
        else{
            return "command not supported: " + command;
        }

        return "command not supported: " + command;
    }

    private void sendCommandToConnetedHost(String command){
        if(connectedHost == null){
            System.out.println("not connected to any host.");
        }
        else
        {
            connectedHost.handleCommand(command);
        }
    }

}
