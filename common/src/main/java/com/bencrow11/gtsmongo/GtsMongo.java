package com.bencrow11.gtsmongo;

import com.bencrow11.gtsmongo.hooks.MongoHistoryItemImp;
import com.bencrow11.gtsmongo.hooks.streamless.StreamlessMongoHistoryProvider;
import com.bencrow11.gtsmongo.hooks.streamless.StreamlessMongoListingProvider;
import com.bencrow11.gtsmongo.hooks.streams.StreamsMongoHistoryProvider;
import com.bencrow11.gtsmongo.hooks.MongoListingImp;
import com.bencrow11.gtsmongo.hooks.streams.StreamsMongoListingProvider;
import com.bencrow11.gtsmongo.mongo.MongoImp;
import org.pokesplash.gts.Gts;
import org.pokesplash.gts.api.provider.*;

public class GtsMongo
{
	public static final String MOD_ID = "gtsmongo";
	public static MongoImp mongo;
	public static final String listingCollection = "listings";
	public static final String historyCollection = "history";
	public static Config config = new Config();

	public static void init() {
		reload();
	}

	public static void reload() {
		config.init();

		mongo = new MongoImp();
		ListingAPI.add(Priority.LOW, new MongoListingImp());
		HistoryAPI.add(Priority.LOW, new MongoHistoryItemImp());

		if (config.isUseStreams()) {
			mongo.runStreams();
			ListingsProviderAPI.add(Priority.LOW, new StreamsMongoListingProvider());
			HistoryProviderAPI.add(Priority.LOW, new StreamsMongoHistoryProvider());
		} else {
			ListingsProviderAPI.add(Priority.LOW, new StreamlessMongoListingProvider());
			HistoryProviderAPI.add(Priority.LOW, new StreamlessMongoHistoryProvider());
		}

		if (config.isMigrateFromJson()) {
			new StreamsMongoListingProvider().migrateToMongo();
			new StreamsMongoHistoryProvider().migrateToMongo();
			Gts.listings.init();
			Gts.history.init();
		}


	}
}
