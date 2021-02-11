package server;

import java.net.InetAddress;
import java.net.ServerSocket;

public class Main {

    class Request {
        String type;
        String key;
        String value;
    }

    public static void main(String[] args){
        String address = "127.0.0.1";
        int port = 23456;
        Database.getInstance();
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address))) {
            System.out.println("Server started!");
            while (true) {
                Session session = new Session(server.accept(), server);
                session.start();
            }
        } catch (Exception ignored) {
        }

    }
}

