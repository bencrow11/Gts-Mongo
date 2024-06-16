package com.bencrow11.gtsmongo.mongo;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.types.Collection;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.UUID;
import java.util.function.Consumer;

public class MongoImp {
    public final MongoClientSettings settings;


    public MongoImp(String username, String password, String host) {
        String connectionString = "mongodb+srv://" + username + ":" + password + "@" + host + "/";

        CodecProvider codecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(codecProvider)
        );

        settings = MongoClientSettings.builder()
                .codecRegistry(codecRegistry)
                .applyConnectionString(new ConnectionString(connectionString))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
    }

    public void add(String json, UUID id, Collection type) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        Document document = Document.parse(json);
        document.append("_id", id.toString());

        MongoCursor<Document> iterator = collection.find(Filters.eq("id", id.toString())).iterator();

        if (!iterator.hasNext()) {
            collection.insertOne(document);
        }
    }

    public Document get(Collection type, UUID id) {
        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        return collection.find(Filters.eq("id", id.toString())).first();
    }

    public void getAll(Collection type, Consumer<Document> consumer) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        collection.find().forEach(consumer);
    }

    public void delete(UUID id, Collection type) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        collection.deleteMany(Filters.eq("id", id.toString()));
    }

    public MongoCollection<Document> getCollection(MongoClient client, Collection collection) {
        MongoDatabase database = client.getDatabase(GtsMongo.db);

        return database.getCollection(
                collection.equals(Collection.LISTING) ? GtsMongo.listingCollection : GtsMongo.historyCollection
        );
    }

    public void runStreams() {
        Runnable listingsListener = new ListingsListener();
        Thread listingsThread = new Thread(listingsListener, "mongo-listings-listener");
        listingsThread.start();

        Runnable historyListener = new HistoryListener();
        Thread historyThread = new Thread(historyListener, "mongo-history-listener");
        historyThread.start();
    }
}
