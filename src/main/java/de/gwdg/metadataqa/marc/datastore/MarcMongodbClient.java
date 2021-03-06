package de.gwdg.metadataqa.marc.datastore;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;

/**
 *
 * @author Péter Király <peter.kiraly at gwdg.de>
 */
public class MarcMongodbClient {

  private MongoClient mongoClient;
  private DB db;

  public MarcMongodbClient(String host, int port, String database) throws UnknownHostException {
    mongoClient = new MongoClient(host, port);
    db = mongoClient.getDB(database);
  }

  public DBCollection getCollection(String collectionName) {
    return db.getCollection(collectionName);
  }

}
