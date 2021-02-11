package server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Session extends Thread {
    private final Socket clientSocket;
    private final ServerSocket serverSocket;

    public Session(Socket clientSocket, ServerSocket serverSocket) {
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            Database database = Database.getInstance();
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
            String requestStr = input.readUTF();
            Gson gson = new Gson();
            JsonObject response = new JsonObject();
            try {
                Main.Request request = gson.fromJson(requestStr, Main.Request.class);
                if (request == null) {
                    return;
                }
                JsonElement retValue;
                switch (request.type) {
                    case "get":
                        retValue = database.get(request.key);
                        if (retValue != null) {
                            response.addProperty("response", "OK");
                            response.add("value", retValue);
                        } else {
                            response.addProperty("response", "ERROR");
//                            response.put("reason", "No such key");
                        }
                        output.writeUTF(gson.toJson(response));
                        break;
                    case "set":
                        retValue = database.set(request.key, request.value);
                        if (retValue == null) {
                            response.addProperty("response", "OK");
                        } else {
                            response.addProperty("response", "ERROR");
//                            response.put("reason", retValue);
                        }

                        output.writeUTF(gson.toJson(response));
                        break;
                    case "delete":
                        if (database.delete(request.key) == null) {
                            response.addProperty("response", "ERROR");
//                            response.put("reason", "No such key");
                        } else {
                            response.addProperty("response", "OK");
                        }
                        output.writeUTF(gson.toJson(response));
                        break;
                    case "exit":
                        response.addProperty("response", "OK");
                        output.writeUTF(gson.toJson(response));
                        serverSocket.close();
                        database.save();
                        break;
                    default:
                        response.addProperty("response", "ERROR");
//                        response.put("reason", "Invalid command");
                        output.writeUTF(gson.toJson(response));
                }
            } catch (Exception e) {
                response.addProperty("response", "ERROR");
                response.addProperty("reason", e.getMessage());
                output.writeUTF(gson.toJson(response));
                e.printStackTrace();
            }
            clientSocket.close();
        } catch (IOException ignored) {
        }
    }
}
