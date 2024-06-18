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
    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;


    /**
     * Constructor to create the db instance.
     */
    public MongoImp() {
        CodecProvider codecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(codecProvider)
        );

        StringBuilder builder = new StringBuilder();
        builder.append(GtsMongo.config.isUseSRV() ? "mongodb+srv://" : "mongodb://");
        if (!GtsMongo.config.getUsername().trim().isEmpty()) {
            builder.append(GtsMongo.config.getUsername());
        }
        if (!GtsMongo.config.getPassword().trim().isEmpty()) {
            builder.append(":").append(GtsMongo.config.getPassword());
        }
        if (!GtsMongo.config.getUsername().trim().isEmpty() || !GtsMongo.config.getPassword().trim().isEmpty()) {
            builder.append("@");
        }

        builder.append(GtsMongo.config.getHost());

        if (!GtsMongo.config.isUseSRV()) {
            builder.append(":").append(GtsMongo.config.getPort());
        }
        builder.append("/");

        MongoClientSettings settings = MongoClientSettings.builder()
                .codecRegistry(codecRegistry)
                .applyConnectionString(new ConnectionString(builder.toString()))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();

        mongoClient = MongoClients.create(settings);
        mongoDatabase = mongoClient.getDatabase(GtsMongo.config.getDatabase());
    }

    /**
     * Adds a JSON object to the database, if the given id doesn't already exist.
     * @param json The JSON object to add.
     * @param id The ID to check doesn't exist.
     * @param type The collection to add the object to.
     */
    public void add(String json, UUID id, Collection type) {

        MongoCollection<Document> collection = getCollection(type);

        Document document = Document.parse(json);
        document.append("_id", id.toString());

        MongoCursor<Document> iterator = collection.find(Filters.eq("id", id.toString())).iterator();

        if (!iterator.hasNext()) {
            collection.insertOne(document);
        }
    }

    /**
     * Method used to replace a document with another using the same ID.
     * @param json The JSON to replace the other document with.
     * @param id The ID of the document to be replaced.
     * @param type The collection type.
     */
    public void replace(String json, UUID id, Collection type) {
        Document document = get(type, id);

        if (document != null) {
            MongoCollection<Document> collection = getCollection(type);

            collection.replaceOne(Filters.eq("id", id.toString()), Document.parse(json));
        }
    }

    /**
     * Gets a document from db using the collection and id specified.
     * @param type The collection to fetch from.
     * @param id The ID of the document to fetch.
     * @return The document found.
     */
    public Document get(Collection type, UUID id) {

        MongoCollection<Document> collection = getCollection(type);

        return collection.find(Filters.eq("id", id.toString())).first();
    }

    /**
     * Fetches all documents from the given collection.
     * @param type The collection to fetch the documents from.
     * @param consumer Callback to process the documents once fetched.
     */
    public void getAll(Collection type, Consumer<Document> consumer) {

        MongoCollection<Document> collection = getCollection(type);

        collection.find().forEach(consumer);
    }

    /**
     * Deletes an object with a given id from a given collection.
     * @param id The ID of the object to delete.
     * @param type The collection to delete the object from.
     */
    public void delete(UUID id, Collection type) {

        MongoCollection<Document> collection = getCollection(type);

        collection.deleteMany(Filters.eq("id", id.toString()));
    }

    /**
     * Fetches the given collection.
     * @param collection The collection to fetch.
     * @return The collection of documents.
     */
    public MongoCollection<Document> getCollection(Collection collection) {

        return mongoDatabase.getCollection(
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
