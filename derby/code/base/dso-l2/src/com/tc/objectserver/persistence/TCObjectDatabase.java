/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;

public interface TCObjectDatabase {

  public Status insert(long id, byte[] b, PersistenceTransaction tx);
  
  public Status update(long id, byte[] b, PersistenceTransaction tx);

  public byte[] get(long id, PersistenceTransaction tx);

  public Status delete(long id, PersistenceTransaction tx);
  
  public ObjectIDSet getAllObjectIds(PersistenceTransaction tx);
}