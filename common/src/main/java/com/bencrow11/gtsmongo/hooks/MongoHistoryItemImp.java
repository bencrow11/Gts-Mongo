package com.bencrow11.gtsmongo.hooks;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import org.pokesplash.gts.api.provider.HistoryInterface;
import org.pokesplash.gts.history.HistoryItem;

/**
 * Implementation of the history interface that writes the history to database instead of to file.
 */
public class MongoHistoryItemImp implements HistoryInterface {
    @Override
    public void write(HistoryItem listing) {

        Gson gson = new Gson();

        GtsMongo.mongo.add(gson.toJson(listing), listing.getId(), Collection.HISTORY);
    }
}
