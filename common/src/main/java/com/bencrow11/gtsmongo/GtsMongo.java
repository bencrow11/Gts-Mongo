package com.bencrow11.gtsmongo;

import com.bencrow11.gtsmongo.hooks.MongoListingImp;
import com.bencrow11.gtsmongo.hooks.MongoListingProvider;
import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.mongodb.client.MongoDatabase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.bson.Document;
import org.pokesplash.gts.Listing.ItemListing;
import org.pokesplash.gts.api.provider.ListingAPI;
import org.pokesplash.gts.api.provider.ListingsProviderAPI;
import org.pokesplash.gts.api.provider.Priority;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GtsMongo
{
	public static final String MOD_ID = "gtsmongo";
	public static MongoImp mongo;
	public static final String db = "gts";
	public static final String listingCollection = "listings";
	public static final String historyCollection = "history";

	public static void init() {
		Logger.getLogger("org.mongodb.driver.client").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.connection").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.protocol").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.authenticator").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.connection.tls").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.operation").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.uri").setLevel(Level.SEVERE);
		Logger.getLogger("org.mongodb.driver.management").setLevel(Level.SEVERE);

		mongo = new MongoImp("bencrow11", "7J4DPpsg8w8tLhTQ",
				"testcluster.icb0dld.mongodb.net");
//		mongo.test();


		ListingsProviderAPI.add(Priority.LOW, new MongoListingProvider());
		ListingAPI.add(Priority.LOW, new MongoListingImp());
	}
}
