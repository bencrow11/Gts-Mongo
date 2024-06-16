package com.bencrow11.gtsmongo.hooks;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import org.pokesplash.gts.Listing.Listing;
import org.pokesplash.gts.api.provider.ListingInterface;

public class MongoListingImp implements ListingInterface {
    @Override
    public void write(Listing listing) {

        Gson gson = new Gson();

        GtsMongo.mongo.add(gson.toJson(listing), Collection.LISTING);
    }

    @Override
    public void delete(Listing listing) {

        GtsMongo.mongo.delete(listing.getId(), Collection.LISTING);
    }
}
