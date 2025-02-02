package com.bencrow11.gtsmongo.hooks.streamless;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.hooks.Migration;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bson.Document;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.Listing.Listing;
import org.pokesplash.gts.history.*;
import org.pokesplash.gts.util.Deserializer;
import org.pokesplash.gts.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of history provider to read files from database rather than file.
 */
public class StreamlessMongoHistoryProvider extends HistoryProvider implements Migration {
    @Override
    public void init() {
        return;
    }

    private Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        // Type adapters help gson deserialize the listings interface.
        builder.registerTypeAdapter(HistoryItem.class, new Deserializer(PokemonHistoryItem.class));
        builder.registerTypeAdapter(HistoryItem.class, new Deserializer(ItemHistoryItem.class));
        return builder.create();
    }

    private HistoryItem convertFromJson(String string) {
        Gson gson = getGson();
        HistoryItem item = gson.fromJson(string, HistoryItem.class);

        item = item.isPokemon() ? gson.fromJson(string, PokemonHistoryItem.class) :
                gson.fromJson(string, ItemHistoryItem.class);

        return item;
    }


    @Override
    public HashMap<UUID, PlayerHistory> getHistory() {

        HashMap<UUID, PlayerHistory> history = new HashMap<>();

        GtsMongo.mongo.getAll(Collection.HISTORY, e -> {
            try {
                HistoryItem item = convertFromJson(e.toJson());

                PlayerHistory playerHistory = history.get(item.getSellerUuid());

                if (playerHistory == null) {
                    playerHistory = new PlayerHistory(item.getSellerUuid());
                }
                List<HistoryItem> historyItems = playerHistory.getListings();

                if (historyItems == null) {
                    historyItems = new ArrayList<>();
                }

                historyItems.add(item);
                history.put(item.getSellerUuid(), playerHistory);
            } catch (Exception ex) {
                Gson gson = getGson();
                HistoryItem item = gson.fromJson(e.toJson(), HistoryItem.class);
                Gts.LOGGER.error("Could not read player GTS History file for " + item.getId());
                ex.printStackTrace();
            }
        });

      return history;
    };

    @Override
    public PlayerHistory getPlayerHistory(UUID uuid) {

        PlayerHistory playerHistory = new PlayerHistory(uuid);

        GtsMongo.mongo.getAllWithField(Collection.HISTORY, "sellerUuid", uuid.toString(), (e -> {
            try {
                HistoryItem item = convertFromJson(e.toJson());

                playerHistory.addHistory(item);
            } catch (Exception ex) {
                Gts.LOGGER.error("Could not read GTS History file for " + e.getString("sellerUuid"));
            }
        }));

        return playerHistory;
    }

    @Override
    public HistoryItem findHistoryById(UUID uuid) {
        Document document = GtsMongo.mongo.get(Collection.HISTORY, uuid);

        if (document == null) {
            return null;
        }

        return convertFromJson(document.toJson());
    }

    @Override
    public void updateHistory(PlayerHistory playerHistory) {
        return;
    }

    @Override
    public void addHistoryItem(Listing item, String buyerName) {
        PlayerHistory playerHistory = new PlayerHistory(item.getSellerUuid());
        playerHistory.addListing(item, buyerName);
    }

    @Override
    public void migrateToMongo() {
        File dir = Utils.checkForDirectory(filePath);

        File[] files = dir.listFiles();

        for (File file : files) {
            File[] playerFiles = file.listFiles();

            // For each file in the players directory
            for (File playerFile : playerFiles) {
                // If it is a file, try read it.
                if (playerFile.isFile()) {
                    Utils.readFileAsync(filePath + file.getName() + "/",
                            playerFile.getName(), el -> {
                                GsonBuilder builder = new GsonBuilder();
                                // Type adapters help gson deserialize the listings interface.
                                builder.registerTypeAdapter(HistoryItem.class, new Deserializer(PokemonHistoryItem.class));
                                builder.registerTypeAdapter(HistoryItem.class, new Deserializer(ItemHistoryItem.class));
                                Gson gson = builder.create();

                                // Try parse the file to a history item.
                                try {
                                    HistoryItem item = gson.fromJson(el, HistoryItem.class);

                                    GtsMongo.mongo.add(el, item.getId(), Collection.HISTORY);

                                } catch (Exception e) {
                                    Gts.LOGGER.error("Could not read player GTS History file for " + file.getName());
                                    e.printStackTrace();
                                }
                            });
                }
            }
        }
    }
}
