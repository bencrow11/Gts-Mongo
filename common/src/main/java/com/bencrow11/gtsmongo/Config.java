package com.bencrow11.gtsmongo;

import com.google.gson.Gson;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.util.Utils;

import java.util.concurrent.CompletableFuture;

/**
 * Config file.
 */
public class Config {
    private String username; // DB username
    private String password; // DB password
    private String host; // DB host string
    private boolean useStreams; // Should the mod use change streams

    /**
     * Constructor to create a default config file.
     */
    public Config() {
        username = "USERNAME";
        password = "PASSWORD";
        host = "DB_NAME.tgjug9y.mongodb.net";
        useStreams = true;
    }

    /**
     * Method to initialize the config.
     */
    public void init() {
        CompletableFuture<Boolean> futureRead = Utils.readFileAsync("/config/gtsmongo/", "config.json",
                el -> {
                    Gson gson = Utils.newGson();
                    Config cfg = gson.fromJson(el, Config.class);
                    username = cfg.getUsername();
                    password = cfg.getPassword();
                    host = cfg.getHost();
                    useStreams = cfg.isUseStreams();
                });

        if (!futureRead.join()) {
            Gts.LOGGER.info("No config.json file found for GTSMongo. Attempting to generate one.");
            CompletableFuture<Boolean> futureWrite = write();

            if (!futureWrite.join()) {
                Gts.LOGGER.fatal("Could not write config for GTSMongo.");
            }
            return;
        }
        Gts.LOGGER.info("GTSMongo config file read successfully.");
    }

    /**
     * Writes the config to file.
     * @return Future true if write was successful, otherwise false.
     */
    public CompletableFuture<Boolean> write() {
        Gson gson = Utils.newGson();
        String data = gson.toJson(this);
        return Utils.writeFileAsync("/config/gtsmongo/", "config.json", data);
    }

    /**
     * Getters
     */

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public boolean isUseStreams() {
        return useStreams;
    }
}

