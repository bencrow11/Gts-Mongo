package com.bencrow11.gtsmongo.mongo;

import com.bencrow11.gtsmongo.GtsMongo;
import com.bencrow11.gtsmongo.mongo.streams.HistoryListener;
import com.bencrow11.gtsmongo.mongo.streams.ListingsListener;
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
    private MongoDatabase mongoDatabase;
    private MongoClient client;


    /**
     * Constructor to create the db instance.
     */
    public MongoImp() {
        CodecProvider codecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(codecProvider)
        );

        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .codecRegistry(codecRegistry)
                    .applyConnectionString(new ConnectionString(GtsMongo.config.getConnectionString()))
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .build();

            client = MongoClients.create(settings);
            mongoDatabase = client.getDatabase(GtsMongo.config.getDatabase());
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }

    /**
     * Adds a JSON object to the database, if the given id doesn't already exist.
     * @param json The JSON object to add.
     * @param id The ID to check doesn't exist.
     * @param type The collection to add the object to.
     */
    public void add(String json, UUID id, Collection type) {

        try {
            MongoCollection<Document> collection = getCollection(type);

            Document document = Document.parse(json);
            document.append("_id", id.toString());

            MongoCursor<Document> iterator = collection.find(Filters.eq("id", id.toString())).iterator();

            if (!iterator.hasNext()) {
                collection.insertOne(document);
            }
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }

    /**
     * Method used to replace a document with another using the same ID.
     * @param json The JSON to replace the other document with.
     * @param id The ID of the document to be replaced.
     * @param type The collection type.
     */
    public void replace(String json, UUID id, Collection type) {
        try {
            Document document = get(type, id);

            if (document != null) {
                MongoCollection<Document> collection = getCollection(type);

                collection.replaceOne(Filters.eq("id", id.toString()), Document.parse(json));
            }
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }

    /**
     * Gets a document from db using the collection and id specified.
     * @param type The collection to fetch from.
     * @param id The ID of the document to fetch.
     * @return The document found.
     */
    public Document get(Collection type, UUID id) {

        try {
            MongoCollection<Document> collection = getCollection(type);

            return collection.find(Filters.eq("id", id.toString())).first();
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Finds all documents with a specified field and value.
     * @param type The collection to fetch the documents from.
     * @param fieldName The name of the field to search for.
     * @param fieldValue The value for the field with the previously given name.
     * @param consumer Callback to process the documents.
     */
    public <T> void getAllWithField(Collection type, String fieldName, T fieldValue, Consumer<Document> consumer) {
        try {
            MongoCollection<Document> collection = getCollection(type);

            collection.find(Filters.eq(fieldName, fieldValue)).forEach(consumer);
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }

    /**
     *
     * @param type
     * @param firstFieldName
     * @param firstFieldValue
     * @param secondFieldName
     * @param secondFieldValue
     * @param consumer
     * @param <T>
     * @param <V>
     */
    public <T, V> void getAllWithTwoField(Collection type,
                                          String firstFieldName, T firstFieldValue,
                                          String secondFieldName, V secondFieldValue,
                                          Consumer<Document> consumer) {
        try {
            MongoCollection<Document> collection = getCollection(type);

            collection.find(
                            Filters.and(Filters.eq(firstFieldName, firstFieldValue),
                                    Filters.eq(secondFieldName, secondFieldValue)))
                    .forEach(consumer);
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }

    /**
     * Fetches all documents from the given collection.
     * @param type The collection to fetch the documents from.
     * @param consumer Callback to process the documents once fetched.
     */
    public void getAll(Collection type, Consumer<Document> consumer) {

        try {
            MongoCollection<Document> collection = getCollection(type);

            collection.find().forEach(consumer);
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }

    /**
     * Deletes an object with a given id from a given collection.
     * @param id The ID of the object to delete.
     * @param type The collection to delete the object from.
     */
    public void delete(UUID id, Collection type) {

        try {
            MongoCollection<Document> collection = getCollection(type);

            collection.deleteMany(Filters.eq("id", id.toString()));
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }

    /**
     * Fetches the given collection.
     * @param collection The collection to fetch.
     * @return The collection of documents.
     */
    public MongoCollection<Document> getCollection(Collection collection) {

        try {
            return mongoDatabase.getCollection(
                    collection.equals(Collection.LISTING) ? GtsMongo.listingCollection : GtsMongo.historyCollection
            );
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
            return null;
        }
    }

    public void closeConnection() {
        Thread.getAllStackTraces().keySet().forEach(thread ->
        {
            if (thread.getName().equalsIgnoreCase("mongo-listings-listener") ||
                    thread.getName().equalsIgnoreCase("mongo-history-listener")) {
                try {
                    thread.interrupt();
                } catch (Exception e) {}
            }
        });
        client.close();
    }

    /**
     * Runs the listeners to sync servers.
     */
    public void runStreams() {
        try {
            Runnable listingsListener = new ListingsListener();
            Thread listingsThread = new Thread(listingsListener, "mongo-listings-listener");
            listingsThread.start();

            Runnable historyListener = new HistoryListener();
            Thread historyThread = new Thread(historyListener, "mongo-history-listener");
            historyThread.start();
        } catch (Exception e) {
            System.out.println("An error occurred with GtsMongo:");
            e.printStackTrace();
        }
    }
}
