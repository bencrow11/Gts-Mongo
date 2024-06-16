package com.bencrow11.gtsmongo;

import com.bencrow11.gtsmongo.hooks.MongoHistoryItemImp;
import com.bencrow11.gtsmongo.hooks.MongoHistoryProvider;
import com.bencrow11.gtsmongo.hooks.MongoListingImp;
import com.bencrow11.gtsmongo.hooks.MongoListingProvider;
import com.bencrow11.gtsmongo.mongo.MongoImp;
import org.pokesplash.gts.api.provider.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GtsMongo
{
	public static final String MOD_ID = "gtsmongo";
	public static MongoImp mongo;
	public static final String db = "gts";
	public static final String listingCollection = "listings";
	public static final String historyCollection = "history";
	public static Config config = new Config();

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

//		mongo.test();

		config.init();

		mongo = new MongoImp(config.getUsername(), config.getPassword(), config.getHost());


		ListingsProviderAPI.add(Priority.LOW, new MongoListingProvider());
		ListingAPI.add(Priority.LOW, new MongoListingImp());
		HistoryProviderAPI.add(Priority.LOW, new MongoHistoryProvider());
		HistoryAPI.add(Priority.LOW, new MongoHistoryItemImp());
	}
}
