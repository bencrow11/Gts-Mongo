package com.bencrow11.gtsmongo;

import com.bencrow11.gtsmongo.types.Collection;
import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoQueryException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.pokesplash.gts.Listing.ItemListing;
import org.pokesplash.gts.Listing.Listing;

import java.util.UUID;
import java.util.function.Consumer;

public class MongoImp {
    private final String connectionString;
    private final CodecRegistry codecRegistry;
    private final MongoClientSettings settings;


    public MongoImp(String username, String password, String clusterName) {
        connectionString = "mongodb+srv://" + username + ":" + password + "@" + clusterName + "/";

        CodecProvider codecProvider = PojoCodecProvider.builder().automatic(true).build();
        codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(codecProvider)
        );

        settings = MongoClientSettings.builder()
                .codecRegistry(codecRegistry)
                .applyConnectionString(new ConnectionString(connectionString))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
    }

    public void test() {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> listings = getCollection(mongoClient, Collection.LISTING);

        ChangeStreamIterable<Document> cursor = listings.watch();

        cursor.forEach(e -> {
            System.out.println(e.getOperationTypeString());
        });
    }

    public void add(String json, Collection type) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        Document document = Document.parse(json);

        collection.insertOne(document);
    }

    public void getAll(Collection type, Consumer<Document> consumer) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        collection.find().forEach(consumer);
    }

    public void delete(UUID id, Collection type) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        collection.deleteOne(Filters.eq("id", id.toString()));
    }

    private MongoCollection<Document> getCollection(MongoClient client, Collection collection) {
        MongoDatabase database = client.getDatabase(GtsMongo.db);

        return database.getCollection(
                collection.equals(Collection.LISTING) ? GtsMongo.listingCollection : GtsMongo.historyCollection
        );
    }
}
