package com.bencrow11.gtsmongo.mongo.streams;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.OperationType;
import org.bson.Document;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.history.HistoryItem;
import org.pokesplash.gts.history.ItemHistoryItem;
import org.pokesplash.gts.history.PlayerHistory;
import org.pokesplash.gts.history.PokemonHistoryItem;
import org.pokesplash.gts.util.Deserializer;

import java.util.Objects;
import java.util.UUID;

/**
 * Listener used to sync the history across servers.
 */
public class HistoryListener implements Runnable {
    @Override
    public void run() {
        MongoCollection<Document> history = GtsMongo.mongo.getCollection(Collection.HISTORY);

        ChangeStreamIterable<Document> cursor = history.watch();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(HistoryItem.class, new Deserializer(PokemonHistoryItem.class));
        builder.registerTypeAdapter(HistoryItem.class, new Deserializer(ItemHistoryItem.class));
        Gson gson = builder.create();

        cursor.forEach(e -> {

            try {
                // Gets the id of the db entry.
                String id = e.getDocumentKey().getString("_id").getValue();

                // If the operation was an insert.
                if (Objects.requireNonNull(e.getOperationType()).equals(OperationType.INSERT)) {

                    // Fetch the document.
                    Document document = GtsMongo.mongo.get(Collection.HISTORY, UUID.fromString(id));

                    // Look for the history item in memory.
                    HistoryItem historyItem = Gts.history.findHistoryById(UUID.fromString(id));

                    // If the document is in the database, but not in memory, add it.
                    if (document != null && historyItem == null) {

                        historyItem  = gson.fromJson(document.toJson(), HistoryItem.class);

                        historyItem = historyItem.isPokemon() ? gson.fromJson(document.toJson(), PokemonHistoryItem.class) :
                                gson.fromJson(document.toJson(), ItemHistoryItem.class);

                        PlayerHistory playerHistory = Gts.history.getPlayerHistory(historyItem.getSellerUuid());

                        playerHistory.addHistory(historyItem);

                        Gts.history.updateHistory(playerHistory);
                    }

                } else {

                    Gts.LOGGER.error("Operation type " + e.getOperationTypeString() +
                            " is not supported for collection: " + GtsMongo.historyCollection);

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}

