package com.bencrow11.gtsmongo;

import com.google.gson.Gson;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.util.Utils;

import java.util.concurrent.CompletableFuture;

/**
 * Config file.
 */
public class Config {
    private String host; // The host of the database.
    private int port; // The database port.
    private String username; // The username used to log in.
    private String password; // The password used to log in.
    private String database; // The database name to store gts stuff in.
    private boolean useSRV; // Should SRV be used.
    private boolean useStreams; // Should the mod use change streams.
    private boolean migrateFromJson; // Should json files be added to the db.

    /**
     * Constructor to create a default config file.
     */
    public Config() {
        host = "localhost";
        port = 27017;
        username = "";
        password = "";
        database = "gts";
        useSRV = false;
        useStreams = true;
        migrateFromJson = false;
    }

    /**
     * Method to initialize the config.
     */
    public void init() {
        CompletableFuture<Boolean> futureRead = Utils.readFileAsync("/config/gtsmongo/", "config.json",
                el -> {
                    Gson gson = Utils.newGson();
                    Config cfg = gson.fromJson(el, Config.class);
                    host = cfg.getHost();
                    port = cfg.getPort();
                    username = cfg.getUsername();
                    password = cfg.getPassword();
                    database = cfg.getDatabase();
                    useSRV = cfg.isUseSRV();
                    useStreams = cfg.isUseStreams();
                    migrateFromJson = cfg.isMigrateFromJson();
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

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUseSRV() {
        return useSRV;
    }

    public String getDatabase() {
        return database;
    }

    public boolean isUseStreams() {
        return useStreams;
    }

    public boolean isMigrateFromJson() {
        return migrateFromJson;
    }
}

