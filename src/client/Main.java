package client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import static com.google.gson.JsonParser.parseString;

public class Main {

    static class Args {

        private static final String requestFilePathPrefix = System.getProperty("user.dir") + File.separator +
//                "JSON Database\\task" + File.separator +
                "src" + File.separator +
                "client" + File.separator +
                "data" + File.separator;

        @Parameter(names = {"-t"})
        private String requestType;

        @Parameter(names = {"-k"})
        private String key;

        @Parameter(names = "-v")
        String value = "";

        @Parameter(names = "-in")
        String requestFileName;

        public void processIn() {
            if (requestFileName != null) {
                File requestFile = new File(requestFilePathPrefix + requestFileName);
                System.out.println("Try to open file: " + requestFilePathPrefix + requestFileName);
                try (FileInputStream fis = new FileInputStream(requestFile)) {
                    Gson gson = new Gson();
                    HashMap<String, String> map;
                    String requestData = new String(fis.readAllBytes());
                    System.out.println("File content: " + requestData);
                    JsonElement requestJson = JsonParser.parseString(requestData);
                    JsonElement curElement;

                    curElement = requestJson.getAsJsonObject().get("type");
                    if (curElement != null) {
                        requestType = curElement.getAsString();
                        System.out.println("Set requestType to " + requestType);
                    }

                    curElement = requestJson.getAsJsonObject().get("key");
                    if (curElement != null) {
                        key = curElement.toString();
                        System.out.println("Set key to " + key);
                    }
                    curElement = requestJson.getAsJsonObject().get("value");
                    if (curElement != null) {
                        value = curElement.toString();
                        System.out.println("Set value to " + value);
                    }
//                    map = gson.fromJson(requestData, HashMap.class);
//                    for (Map.Entry entry : map.entrySet()
//                    ) {
//                        switch ((String) entry.getKey()) {
//                            case "type":
//                                System.out.println("Setting type");
//                                requestType = (String) entry.getValue();
//                                break;
//                            case "key":
//                                System.out.println("Setting key");
//                                key = (String) entry.getValue();
//                                break;
//                            case "value":
//                                System.out.println("Setting value");
//                                value = (String) entry.getValue();
//                                break;
//                        }
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void main(String[] args) throws IOException {
        Args arguments = new Args();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);
        arguments.processIn();
        String address = "127.0.0.1";
        int port = 23456;
        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();
        try (Socket socket = new Socket(InetAddress.getByName(address), port)) {
            System.out.println("Client started!");
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());

            if (arguments.requestType == null) {
                System.out.println("request type not found in params: " + Arrays.toString(args));
                return;
            }

            switch (arguments.requestType) {
                case "get":
                case "delete":
                    map.put("type", arguments.requestType);
                    map.put("key", arguments.key);
                    break;
                case "set":
                    map.put("type", arguments.requestType);
                    map.put("key", arguments.key);
                    map.put("value", arguments.value);
                    break;
                case "exit":
                    map.put("type", arguments.requestType);
                    break;
                default:
                    return;
            }
            String requestBody = gson.toJson(map);
            output.writeUTF(requestBody);
            System.out.println("Sent: " + requestBody);
            String resultStr = input.readUTF();
            System.out.println("Received: " + resultStr);
        }
    }
}
