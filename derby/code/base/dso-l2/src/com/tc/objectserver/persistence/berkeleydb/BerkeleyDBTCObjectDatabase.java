/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.berkeleydb;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.TCObjectDatabase;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.util.Conversion;
import com.tc.util.ObjectIDSet;

public class BerkeleyDBTCObjectDatabase extends AbstractBerkeleyDatabase implements TCObjectDatabase {
  private static final TCLogger logger = TCLogging.getLogger(BerkeleyDBTCObjectDatabase.class);

  public BerkeleyDBTCObjectDatabase(Database db) {
    super(db);
  }

  public Status delete(long id, PersistenceTransaction tx) {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(id));
    OperationStatus status = this.db.delete(pt2nt(tx), key);
    if (!(OperationStatus.NOTFOUND.equals(status) || OperationStatus.SUCCESS.equals(status))) { return Status.NOT_SUCCESS; }
    return Status.SUCCESS;
  }

  public byte[] get(long id, PersistenceTransaction tx) {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(id));
    DatabaseEntry value = new DatabaseEntry();
    OperationStatus status = this.db.get(pt2nt(tx), key, value, LockMode.DEFAULT);
    if (OperationStatus.SUCCESS.equals(status)) {
      return value.getData();
    } else if (OperationStatus.NOTFOUND.equals(status)) { return null; }

    throw new DBException("Error retrieving object id: " + id + "; status: " + status);
  }

  private Status put(long id, byte[] val, PersistenceTransaction tx) {
    DatabaseEntry key = new DatabaseEntry();
    key.setData(Conversion.long2Bytes(id));

    DatabaseEntry value = new DatabaseEntry();
    value.setData(val);
    OperationStatus status = this.db.put(pt2nt(tx), key, value);
    return status.equals(OperationStatus.SUCCESS) ? Status.SUCCESS : Status.NOT_SUCCESS;
  }

  public ObjectIDSet getAllObjectIds(PersistenceTransaction tx) {
    Cursor cursor = null;
    CursorConfig dBCursorConfig = new CursorConfig();
    dBCursorConfig.setReadCommitted(true);
    ObjectIDSet tmp = new ObjectIDSet();
    try {
      cursor = db.openCursor(pt2nt(tx), dBCursorConfig);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        tmp.add(new ObjectID(Conversion.bytes2Long(key.getData())));
      }
    } catch (Throwable t) {
      logger.error("Error Reading Object IDs", t);
    } finally {
      safeClose(cursor);
      safeCommit(tx);
    }
    return tmp;
  }

  protected void safeCommit(PersistenceTransaction tx) {
    if (tx == null) return;
    try {
      tx.commit();
    } catch (Throwable t) {
      logger.error("Error Committing Transaction", t);
    }
  }

  protected void safeClose(Cursor c) {
    if (c == null) return;

    try {
      c.close();
    } catch (Throwable e) {
      logger.error("Error closing cursor", e);
    }
  }

  public Status insert(long id, byte[] b, PersistenceTransaction tx) {
    return put(id, b, tx);
  }

  public Status update(long id, byte[] b, PersistenceTransaction tx) {
    return put(id, b, tx);
  }
}