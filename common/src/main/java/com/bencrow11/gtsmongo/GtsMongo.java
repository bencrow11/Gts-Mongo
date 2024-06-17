package com.bencrow11.gtsmongo;

import com.bencrow11.gtsmongo.hooks.MongoHistoryItemImp;
import com.bencrow11.gtsmongo.hooks.MongoHistoryProvider;
import com.bencrow11.gtsmongo.hooks.MongoListingImp;
import com.bencrow11.gtsmongo.hooks.MongoListingProvider;
import com.bencrow11.gtsmongo.mongo.MongoImp;
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

		ListingsProviderAPI.add(Priority.LOW, new MongoListingProvider());
		ListingAPI.add(Priority.LOW, new MongoListingImp());
		HistoryProviderAPI.add(Priority.LOW, new MongoHistoryProvider());
		HistoryAPI.add(Priority.LOW, new MongoHistoryItemImp());

		if (config.isUseStreams()) {
			mongo.runStreams();
		}
	}
}
