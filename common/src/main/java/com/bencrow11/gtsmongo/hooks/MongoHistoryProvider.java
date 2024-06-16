package com.bencrow11.gtsmongo.hooks;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.history.*;
import org.pokesplash.gts.util.Deserializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Implementation of history provider to read files from database rather than file.
 */
public class MongoHistoryProvider extends HistoryProvider {
    @Override
    public void init() {

        HashMap<UUID, ArrayList<HistoryItem>> items = new HashMap<>();

        GtsMongo.mongo.getAll(Collection.HISTORY, e -> {
            GsonBuilder builder = new GsonBuilder();
            // Type adapters help gson deserialize the listings interface.
            builder.registerTypeAdapter(HistoryItem.class, new Deserializer(PokemonHistoryItem.class));
            builder.registerTypeAdapter(HistoryItem.class, new Deserializer(ItemHistoryItem.class));
            Gson gson = builder.create();

            try {
                HistoryItem item = gson.fromJson(e.toJson(), HistoryItem.class);

                item = item.isPokemon() ? gson.fromJson(e.toJson(), PokemonHistoryItem.class) :
                        gson.fromJson(e.toJson(), ItemHistoryItem.class);

                ArrayList<HistoryItem> historyItems = items.get(item.getSellerUuid());

                if (historyItems == null) {
                    historyItems = new ArrayList<>();
                }

                historyItems.add(item);
                items.put(item.getSellerUuid(), historyItems);
            } catch (Exception ex) {
                HistoryItem item = gson.fromJson(e.toJson(), HistoryItem.class);
                Gts.LOGGER.error("Could not read player GTS History file for " + item.getId());
                ex.printStackTrace();
            }
        });

        super.history = new HashMap<>();

        for (UUID uuid : items.keySet()) {
            super.history.put(uuid, new PlayerHistory(uuid, items.get(uuid)));
        }
    }
}
