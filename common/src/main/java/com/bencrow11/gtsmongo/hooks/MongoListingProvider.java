package com.bencrow11.gtsmongo.hooks;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.Listing.ItemListing;
import org.pokesplash.gts.Listing.Listing;
import org.pokesplash.gts.Listing.ListingsProvider;
import org.pokesplash.gts.Listing.PokemonListing;
import org.pokesplash.gts.util.Deserializer;

import java.util.Date;

/**
 * Implementation of listing provider that overrides method to read listings from db rather than file.
 */
public class MongoListingProvider extends ListingsProvider {
    @Override
    public void init() {
        GtsMongo.mongo.getAll(Collection.LISTING, el -> {
            GsonBuilder builder = new GsonBuilder();
            // Type adapters help gson deserialize the listings interface.
            builder.registerTypeAdapter(Listing.class, new Deserializer(PokemonListing.class));
            builder.registerTypeAdapter(Listing.class, new Deserializer(ItemListing.class));
            Gson gson = builder.create();

            Listing listing = gson.fromJson(el.toJson(), Listing.class);

            listing = listing.isPokemon() ? gson.fromJson(el.toJson(), PokemonListing.class) :
                    gson.fromJson(el.toJson(), ItemListing.class);

            if (listing.getEndTime() > new Date().getTime() ||
                    listing.getEndTime() == -1) {
                listings.add(listing);
                Gts.timers.addTimer(listing);
            } else {
                addExpiredListing(listing);
            }
        });
    }
}
