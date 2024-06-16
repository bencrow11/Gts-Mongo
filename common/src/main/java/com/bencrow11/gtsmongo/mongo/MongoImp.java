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

/**
 * Class used to implement methods to read / write to the mongoDB database.
 */
public class MongoImp {
    public final MongoClientSettings settings; // Save the settings.


    /**
     * Constructor that takes the db details to form a connection.
     * @param username The username to log into the db.
     * @param password The password for the username.
     * @param host The host address of the database.
     */
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

    /**
     * Adds a JSON object to the database, if the given id doesn't already exist.
     * @param json The JSON object to add.
     * @param id The ID to check doesn't exist.
     * @param type The collection to add the object to.
     */
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

    /**
     * Gets a document from db using the collection and id specified.
     * @param type The collection to fetch from.
     * @param id The ID of the document to fetch.
     * @return The document found.
     */
    public Document get(Collection type, UUID id) {
        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        return collection.find(Filters.eq("id", id.toString())).first();
    }

    /**
     * Fetches all documents from the given collection.
     * @param type The collection to fetch the documents from.
     * @param consumer Callback to process the documents once fetched.
     */
    public void getAll(Collection type, Consumer<Document> consumer) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        collection.find().forEach(consumer);
    }

    /**
     * Deletes an object with a given id from a given collection.
     * @param id The ID of the object to delete.
     * @param type The collection to delete the object from.
     */
    public void delete(UUID id, Collection type) {

        MongoClient mongoClient = MongoClients.create(settings);

        MongoCollection<Document> collection = getCollection(mongoClient, type);

        collection.deleteMany(Filters.eq("id", id.toString()));
    }

    /**
     * Fetches the given collection.
     * @param client The mongoclient used to fetch the collection.
     * @param collection The collection to fetch.
     * @return The collection of documents.
     */
    public MongoCollection<Document> getCollection(MongoClient client, Collection collection) {
        MongoDatabase database = client.getDatabase(GtsMongo.db);

        return database.getCollection(
                collection.equals(Collection.LISTING) ? GtsMongo.listingCollection : GtsMongo.historyCollection
        );
    }

    /**
     * Runs the listeners to sync servers.
     */
    public void runStreams() {
        Runnable listingsListener = new ListingsListener();
        Thread listingsThread = new Thread(listingsListener, "mongo-listings-listener");
        listingsThread.start();

        Runnable historyListener = new HistoryListener();
        Thread historyThread = new Thread(historyListener, "mongo-history-listener");
        historyThread.start();
    }
}
