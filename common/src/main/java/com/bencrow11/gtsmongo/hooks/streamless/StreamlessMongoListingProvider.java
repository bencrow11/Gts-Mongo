package com.bencrow11.gtsmongo.hooks.streamless;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.hooks.Migration;
import com.bencrow11.gtsmongo.hooks.MongoListingImp;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bson.Document;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.Listing.ItemListing;
import org.pokesplash.gts.Listing.Listing;
import org.pokesplash.gts.Listing.ListingsProvider;
import org.pokesplash.gts.Listing.PokemonListing;
import org.pokesplash.gts.api.provider.ListingAPI;
import org.pokesplash.gts.util.Deserializer;
import org.pokesplash.gts.util.Utils;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of listing provider that overrides method to read listings from db rather than file.
 */
public class StreamlessMongoListingProvider extends ListingsProvider implements Migration {

    @Override
    public void init() {
        return;
    }

    private Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        // Type adapters help gson deserialize the listings interface.
        builder.registerTypeAdapter(Listing.class, new Deserializer(PokemonListing.class));
        builder.registerTypeAdapter(Listing.class, new Deserializer(ItemListing.class));
        return builder.create();
    }

    private Listing convertFromJson(String string) {
        Gson gson = getGson();
        Listing listing = gson.fromJson(string, Listing.class);

        listing = listing.isPokemon() ? gson.fromJson(string, PokemonListing.class) :
                gson.fromJson(string, ItemListing.class);

        return listing;
    }

    @Override
    public void relistAllExpiredListings(UUID player) {
        GtsMongo.mongo.getAllWithField(Collection.LISTING, "sellerUuid", player.toString(), (el) -> {
            Listing listing = convertFromJson(el.toJson());
            listing.renewEndTime();
            ListingAPI.getHighestPriority().write(listing);
        });
    }

    @Override
    public List<Listing> getListings() {
        List<Listing> listings = new ArrayList<>();
        GtsMongo.mongo.getAll(Collection.LISTING, el -> {
            Listing listing = convertFromJson(el.toJson());
            if (listing.getEndTime() > new Date().getTime()) {
                listings.add(listing);
            }
        });
        return listings;
    }

    @Override
    public List<PokemonListing> getPokemonListings() {
        List<PokemonListing> listings = new ArrayList<>();
        GtsMongo.mongo.getAllWithField(Collection.LISTING, "isPokemon", true, (el) -> {
            Gson gson = getGson();
            PokemonListing listing = gson.fromJson(el.toJson(), PokemonListing.class);
            if (listing.getEndTime() > new Date().getTime()) {
                listings.add(listing);
            }
        });
        return listings;
    }

    @Override
    public List<Listing> getListingsByPlayer(UUID uuid) {
        List<Listing> listings = new ArrayList<>();
        GtsMongo.mongo.getAllWithField(Collection.LISTING, "sellerUuid", uuid.toString(), (el) -> {
            Listing listing = convertFromJson(el.toJson());
            if (listing.getEndTime() > new Date().getTime()) {
                listings.add(listing);
            }
        });
        return listings;
    }

    @Override
    public List<PokemonListing> getPokemonListingsByPlayer(UUID uuid) {
        List<PokemonListing> listings = new ArrayList<>();
        GtsMongo.mongo.getAllWithTwoField(Collection.LISTING,
                "sellerUuid", uuid.toString(),
                "isPokemon", true,
                (el) -> {
            Gson gson = getGson();
            PokemonListing listing = gson.fromJson(el.toJson(), PokemonListing.class);
            if (listing.getEndTime() > new Date().getTime()) {
                listings.add(listing);
            }
        });
        return listings;
    }

    @Override
    public List<ItemListing> getItemListings() {
        List<ItemListing> listings = new ArrayList<>();
        GtsMongo.mongo.getAllWithField(Collection.LISTING, "isPokemon", false, (el) -> {
            Gson gson = getGson();
            ItemListing listing = gson.fromJson(el.toJson(), ItemListing.class);
            if (listing.getEndTime() > new Date().getTime()) {
                listings.add(listing);
            }
        });
        return listings;
    }

    @Override
    public List<ItemListing> getItemListingsByPlayer(UUID uuid) {
        List<ItemListing> listings = new ArrayList<>();
        GtsMongo.mongo.getAllWithTwoField(Collection.LISTING,
                "sellerUuid", uuid.toString(),
                "isPokemon", false,
                (el) -> {
                    Gson gson = getGson();
                    ItemListing listing = gson.fromJson(el.toJson(), ItemListing.class);
                    if (listing.getEndTime() > new Date().getTime()) {
                        listings.add(listing);
                    }
                });
        return listings;
    }

    @Override
    public boolean addListing(Listing listing) throws IllegalArgumentException {
        Document document = GtsMongo.mongo.get(Collection.LISTING, listing.getId());
        if (document != null) {
            throw new IllegalArgumentException("This listing already exists!");
        }
        return listing.write(null);
    }

    @Override
    public boolean removeListing(Listing listing) throws IllegalArgumentException {
        Document document = GtsMongo.mongo.get(Collection.LISTING, listing.getId());
        if (document == null) {
            throw new IllegalArgumentException("No listing with the UUID " + listing.getId() + " exists.");
        }
        return listing.delete(null);
    }

    @Override
    public boolean hasExpiredListings(UUID playerUUID) {

        AtomicBoolean hasExpiredListings = new AtomicBoolean(false);

        GtsMongo.mongo.getAllWithField(Collection.LISTING, "sellerUuid", playerUUID.toString(), (el) -> {
            Gson gson = getGson();
            Listing listing = convertFromJson(el.toJson());
            if (listing.getEndTime() < new Date().getTime()) {
                hasExpiredListings.set(true);
            }
        });

        return hasExpiredListings.get();
    }

    @Override
    public boolean addExpiredListing(Listing listing) {
        new MongoListingImp().write(listing);
        return true;
    }

    @Override
    public boolean removeExpiredListing(Listing listing) {
        GtsMongo.mongo.delete(listing.getId(), Collection.LISTING);
        return true;
    }

    @Override
    public Listing getActiveListingById(UUID id) {
        Document document = GtsMongo.mongo.get(Collection.LISTING, id);
        if (document != null) {
            Listing listing = convertFromJson(document.toJson());
            if (listing.getEndTime() > new Date().getTime()) {
                return listing;
            }
        }
        return null;
    }

    @Override
    public Listing getExpiredListingById(UUID id) {
        Document document = GtsMongo.mongo.get(Collection.LISTING, id);
        if (document != null) {
            Listing listing = convertFromJson(document.toJson());
            if (listing.getEndTime() < new Date().getTime()) {
                return listing;
            }
        }
        return null;
    }

    @Override
    public Listing getListingById(UUID id) {
        Document document = GtsMongo.mongo.get(Collection.LISTING, id);
        if (document == null) {
            return null;
        }

        Listing listing = convertFromJson(document.toJson());
        return listing;
    }

    @Override
    public List<Listing> getExpiredListingsOfPlayer(UUID player) {

        ArrayList<Listing> listings = new ArrayList<>();

        GtsMongo.mongo.getAllWithField(Collection.LISTING, "sellerUuid", player.toString(), (el) -> {
            Listing listing = convertFromJson(el.toJson());
            if (listing.getEndTime() < new Date().getTime()) {
                listings.add(listing);
            }
        });

        return listings;
    }

    @Override
    public HashMap<UUID, ArrayList<Listing>> getExpiredListings() {

        HashMap<UUID, ArrayList<Listing>> expiredListings = new HashMap<>();

        GtsMongo.mongo.getAll(Collection.LISTING, el -> {
            Listing listing = convertFromJson(el.toJson());

            if (listing.getEndTime() < new Date().getTime()) {

                ArrayList<Listing> listings = expiredListings.get(listing.getSellerUuid());
                if (listings == null) {
                    listings = new ArrayList<>();
                }

                listings.add(listing);
                expiredListings.put(listing.getSellerUuid(), listings);
            }
        });

        return expiredListings;
    }

    @Override
    public void check() {
        return;
    }

    @Override
    public void migrateToMongo() {
        File dir = Utils.checkForDirectory(Gts.LISTING_FILE_PATH);

        String[] list = dir.list();

        if (list.length != 0) {
            for (String file : list) {
                Utils.readFileAsync(Gts.LISTING_FILE_PATH, file, el -> {
                    GsonBuilder builder = new GsonBuilder();
                    // Type adapters help gson deserialize the listings interface.
                    builder.registerTypeAdapter(Listing.class, new Deserializer(PokemonListing.class));
                    builder.registerTypeAdapter(Listing.class, new Deserializer(ItemListing.class));
                    Gson gson = builder.create();

                    Listing listing = gson.fromJson(el, Listing.class);

                    GtsMongo.mongo.add(el, listing.getId(), Collection.LISTING);
                });
            }
        }

    }
}
