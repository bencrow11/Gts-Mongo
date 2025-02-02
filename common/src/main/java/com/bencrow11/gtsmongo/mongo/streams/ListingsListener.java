package com.bencrow11.gtsmongo.mongo.streams;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.Listing.ItemListing;
import org.pokesplash.gts.Listing.Listing;
import org.pokesplash.gts.Listing.PokemonListing;
import org.pokesplash.gts.util.Deserializer;
import org.pokesplash.gts.util.Utils;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Listener to sync listings across servers.
 */
public class ListingsListener implements Runnable {
    @Override
    public void run() {

        MongoCollection<Document> listings = GtsMongo.mongo.getCollection(Collection.LISTING);

        ChangeStreamIterable<Document> cursor = listings.watch();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Listing.class, new Deserializer(PokemonListing.class));
        builder.registerTypeAdapter(Listing.class, new Deserializer(ItemListing.class));
        Gson gson = builder.create();

        cursor.forEach(e -> {

            try {
                // Get the db id of the listing.
                String id = e.getDocumentKey().getString("_id").getValue();

                // Switch depending if the operation was a delete or insert.
                switch (Objects.requireNonNull(e.getOperationType())) {
                    case DELETE:

                        Document deletedDocument = GtsMongo.mongo.get(Collection.LISTING, UUID.fromString(id));

                        Listing deletedActiveListing = Gts.listings.getActiveListingById(UUID.fromString(id));
                        Listing deletedExpiredListing = Gts.listings.getExpiredListingById(UUID.fromString(id));

                        // If the document has been deleted from db but not from memory
                        if (deletedDocument == null && deletedActiveListing != null) {
                            Gts.listings.removeListing(deletedActiveListing);

                        } else if (deletedDocument == null && deletedExpiredListing != null) {
                            Gts.listings.removeExpiredListing(deletedExpiredListing);
                        }

                        break;

                    case INSERT:

                        Document addedDocument = GtsMongo.mongo.get(Collection.LISTING, UUID.fromString(id));

                        Listing addedListing = Gts.listings.getListingById(UUID.fromString(id));

                        // If the document has been added to db but not in memory.
                        if (addedDocument != null && addedListing == null) {

                            addedListing  = gson.fromJson(addedDocument.toJson(), Listing.class);

                            addedListing = addedListing.isPokemon() ? gson.fromJson(addedDocument.toJson(), PokemonListing.class) :
                                    gson.fromJson(addedDocument.toJson(), ItemListing.class);



                            if (addedListing.getEndTime() > new Date().getTime()) {
                                Gts.listings.addListing(addedListing);

                                // Broadcast the new listing on the server.
                                if (Gts.config.isBroadcastListings()) {

                                    if (addedListing.isPokemon()) {

                                        PokemonListing pokemonListing = gson.fromJson(addedDocument.toJson(), PokemonListing.class);

                                        Utils.broadcastClickable(Utils.formatPlaceholders(Gts.language.getNewListingBroadcast(),
                                                        0, pokemonListing.getListing().getDisplayName().getString(),
                                                        pokemonListing.getSellerName(), null),
                                                "/gts " + pokemonListing.getId());
                                    } else {

                                        ItemListing itemListing = gson.fromJson(addedDocument.toJson(), ItemListing.class);

                                        Utils.broadcastClickable(Utils.formatPlaceholders(Gts.language.getNewListingBroadcast(),
                                                        0, itemListing.getListing().getDisplayName().getString(),
                                                        itemListing.getSellerName(), null),
                                                "/gts " + itemListing.getId());
                                    }
                                }
                            } else {
                                Gts.listings.addExpiredListing(addedListing);
                            }


                        }

                        break;

                    case REPLACE:

                        Document replacedDocument = GtsMongo.mongo.get(Collection.LISTING, UUID.fromString(id));

                        if (replacedDocument != null) {

                            Listing activeReplacedListing = Gts.listings.getActiveListingById(UUID.fromString(id));

                            Listing expiredReplacedListing = Gts.listings.getExpiredListingById(UUID.fromString(id));

                            if (activeReplacedListing != null) {
                                Gts.listings.removeListing(activeReplacedListing);
                            }

                            if (expiredReplacedListing != null) {
                                Gts.listings.removeExpiredListing(expiredReplacedListing);
                            }

                            Listing replacedListing  = gson.fromJson(replacedDocument.toJson(), Listing.class);

                            replacedListing = replacedListing.isPokemon() ? gson.fromJson(replacedDocument.toJson(), PokemonListing.class) :
                                    gson.fromJson(replacedDocument.toJson(), ItemListing.class);

                            if (replacedListing.getEndTime() > new Date().getTime()) {

                                Gts.listings.addListing(replacedListing);

                            } else {

                                Gts.listings.addExpiredListing(replacedListing);
                            }


                        }

                        break;

                    default:

                        Gts.LOGGER.error("Operation type " + e.getOperationTypeString() +
                                " is not supported for collection: " + GtsMongo.listingCollection);

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}

