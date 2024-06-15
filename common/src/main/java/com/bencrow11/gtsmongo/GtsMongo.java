package com.bencrow11.gtsmongo;

public class GtsMongo
{
	public static final String MOD_ID = "gtsmongo";
	public static MongoImp mongo;

	public static void init() {
		mongo = new MongoImp("bencrow11", "7J4DPpsg8w8tLhTQ",
				"testcluster.icb0dld.mongodb.net");
		mongo.test();
	}
}
