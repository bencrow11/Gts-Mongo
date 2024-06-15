package com.bencrow11.gtsmongo;

import com.mongodb.*;
import com.mongodb.client.*;
import org.bson.Document;

public class MongoImp {
    private final String connectionString;

    public MongoImp(String username, String password, String clusterName) {
        connectionString = "mongodb+srv://" + username + ":" + password + "@" + clusterName + "/";
    }

    public void test() {
        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();

        // Creates a new client and connects to the server.
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            try {
                MongoDatabase database = mongoClient.getDatabase("test");

                database.createCollection("testCollection");

                MongoCollection<Document> collection = database.getCollection("testCollection");


//                collection.insertOne(new Document("name", "testDocument"));


                FindIterable<Document> iterable = collection.find();
                iterable.forEach(document -> {
                    System.out.printf("DOCUMENT");
                    System.out.println(document.toJson());
                    System.out.printf(document.get("_id").toString());
                });
                mongoClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
