package com.bencrow11.gtsmongo.mongo;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.OperationType;
import org.bson.Document;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.Listing.ItemListing;
import org.pokesplash.gts.Listing.Listing;
import org.pokesplash.gts.Listing.PokemonListing;
import org.pokesplash.gts.history.HistoryItem;
import org.pokesplash.gts.history.ItemHistoryItem;
import org.pokesplash.gts.history.PlayerHistory;
import org.pokesplash.gts.history.PokemonHistoryItem;
import org.pokesplash.gts.util.Deserializer;

import java.util.Objects;
import java.util.UUID;

public class HistoryListener implements Runnable {
    @Override
    public void run() {
        MongoClient mongoClient = MongoClients.create(GtsMongo.mongo.settings);

        MongoCollection<Document> history = GtsMongo.mongo.getCollection(mongoClient, Collection.HISTORY);

        ChangeStreamIterable<Document> cursor = history.watch();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(HistoryItem.class, new Deserializer(PokemonHistoryItem.class));
        builder.registerTypeAdapter(HistoryItem.class, new Deserializer(ItemHistoryItem.class));
        Gson gson = builder.create();

        cursor.forEach(e -> {

            try {
                String id = e.getDocumentKey().getString("_id").getValue();

                if (Objects.requireNonNull(e.getOperationType()).equals(OperationType.INSERT)) {

                    Document document = GtsMongo.mongo.get(Collection.HISTORY, UUID.fromString(id));

                    HistoryItem historyItem = Gts.history.findHistoryById(UUID.fromString(id));

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

