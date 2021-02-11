package server;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.StreamSupport;

public class Database {
    private static final String dbFilePath = System.getProperty("user.dir") + File.separator +
            "src" + File.separator +
            "server" + File.separator +
            "data" + File.separator + "db.json";

    private static final Database instance = new Database();

    ReadWriteLock lock;

//    HashMap<String, String> data;
    JsonObject data;

    static Database getInstance() {
        return instance;
    }

    private Database() {
        lock = new ReentrantReadWriteLock();
        load();
    }

    private String[] prepareKeyPath(String key) {
        try {
            JsonElement jsonPath = JsonParser.parseString(key);
            if (jsonPath.isJsonArray()) {
                return StreamSupport.stream(jsonPath.getAsJsonArray().spliterator(), false).map(JsonElement::getAsString).toArray(String[]::new);
            }
        }catch (IllegalStateException e) {
            return null;
        }
        return null;
    }

    private JsonElement findKey(String[] pathArray, boolean create) {
        if (pathArray != null) {
            JsonElement curElement = data;
            JsonElement foundMember;
            for (int i = 0; i < pathArray.length - 1; i++) {
                if (!curElement.isJsonObject()) {
                    return null;
                }
                if ((foundMember = curElement.getAsJsonObject().get(pathArray[i])) != null) {
                    curElement = foundMember.getAsJsonObject();
                } else {
                    if (!create) {
                        return null;
                    }
                    JsonObject tmpObject = new JsonObject();
                    curElement
                            .getAsJsonObject()
                            .add(pathArray[i], tmpObject);
                    curElement = tmpObject;
                }
            }
            return curElement;
        } else {
            return null;
        }
    }

    public JsonElement set(String key, String value) {
        JsonObject retValue = null;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        String[] pathArray = prepareKeyPath(key);
        JsonElement jsonValue;
        JsonElement curElement;
        if (pathArray == null) {
            key = key.replaceAll("(^\")|(\"$)", "");
            curElement = data;
        } else {
            key = pathArray[pathArray.length - 1];
            curElement = findKey(pathArray, true);
        }
        if (curElement != null) {
            try {
                jsonValue = JsonParser.parseString(value);
                curElement.getAsJsonObject().add(key, jsonValue);
            } catch (JsonSyntaxException e) {
                curElement.getAsJsonObject().addProperty(key, value);
            }

        } else {
            retValue = new JsonObject();
            retValue.addProperty("reason", "Invalid key");
        }
        writeLock.unlock();
        return retValue;
    }

    public JsonElement get(String key) {
        JsonElement value = null;
        Lock readLock = lock.readLock();
        readLock.lock();
        String[] pathArray = prepareKeyPath(key);
        JsonElement curElement = findKey(pathArray, false);
        JsonElement foundItem;
        if (curElement != null && curElement.isJsonObject()) {
            foundItem = curElement.getAsJsonObject().get(pathArray[pathArray.length - 1]);
            if (foundItem != null) {
                value =  foundItem;
            }
        }
        readLock.unlock();
        return value;
    }

    public JsonElement delete(String key) {
        JsonElement value = null;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        String[] pathArray = prepareKeyPath(key);
        JsonElement curElement = findKey(pathArray, false);
        if (curElement != null && curElement.isJsonObject()) {
            value = curElement.getAsJsonObject().remove(pathArray[pathArray.length - 1]);
        }
        writeLock.unlock();
        return value;
    }

    public void load() {
        Lock readLock = lock.writeLock();
        readLock.lock();
        Path path = Paths.get(dbFilePath);

        try (Reader reader = Files.newBufferedReader(path,
                StandardCharsets.UTF_8)) {
            JsonElement parsedData = JsonParser.parseReader(reader);
            data = parsedData.isJsonObject()?parsedData.getAsJsonObject():new JsonObject();
//            data = gson.fromJson(new String(fis.readAllBytes()), HashMap.class);

        } catch (IOException ignored) {
//            data = new HashMap<>();
            data = new JsonObject();
        }
        readLock.unlock();
    }

    public void save() {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        Gson gson = new Gson();
        Path path = Paths.get(dbFilePath);
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)){
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeLock.unlock();
    }
}
