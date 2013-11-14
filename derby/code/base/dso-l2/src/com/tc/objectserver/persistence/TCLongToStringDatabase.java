/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import gnu.trove.TLongObjectHashMap;

public interface TCLongToStringDatabase {
  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target, PersistenceTransaction tx);

  public Status put(long val, String string, PersistenceTransaction tx);
}