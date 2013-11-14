/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.locks.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.tx.MemcacheRootDNA;
import com.tc.objectserver.tx.ServerMapEvictionDNA;
import com.tc.objectserver.tx.ServerMapEvictionMetaDataReader;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.util.SequenceID;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ServerTransactionFactory {

  private static final LockID[]        NULL_LOCK_ID          = new LockID[0];
  private static final long[]          EMPTY_HIGH_WATER_MARK = new long[0];
  private static final DmiDescriptor[] NULL_DMI_DESCRIPTOR   = new DmiDescriptor[0];

  private final AtomicLong             tid                   = new AtomicLong();

  public ServerTransaction createTxnFrom(final ObjectSyncMessage syncMsg) {
    final ObjectSyncServerTransaction txn = new ObjectSyncServerTransaction(syncMsg.getServerTransactionID(),
                                                                            syncMsg.getOids(), syncMsg.getDnaCount(),
                                                                            syncMsg.getDNAs(), syncMsg.getRootsMap(),
                                                                            syncMsg.messageFrom());
    return txn;
  }

  private TransactionID getNextTransactionID() {
    return new TransactionID(this.tid.incrementAndGet());
  }

  /**
   * Since the transaction IDs that are generated by this factory are not coming from a persisted DB this will
   * regenerate the same TransactionID on server restart, but the nodeID should be different.
   */
  public ServerTransactionID getNextServerTransactionID(final NodeID localNodeID) {
    return new ServerTransactionID(localNodeID, getNextTransactionID());
  }

  public ServerTransaction createServerMapEvictionTransactionFor(final NodeID localNodeID, final ObjectID oid,
                                                                 final String className, final String loaderDesc,
                                                                 final Map candidates,
                                                                 final ObjectStringSerializer serializer,
                                                                 final String cacheName) {
    return new ServerTransactionImpl(TxnBatchID.NULL_BATCH_ID, getNextTransactionID(), SequenceID.NULL_ID,
                                     NULL_LOCK_ID, localNodeID,
                                     Collections.singletonList(createServerMapEvictionDNAFor(oid, className,
                                                                                             loaderDesc, candidates,
                                                                                             cacheName)), serializer,
                                     Collections.EMPTY_MAP, TxnType.NORMAL, Collections.EMPTY_LIST,
                                     NULL_DMI_DESCRIPTOR,
                                     new MetaDataReader[] { createEvictionMetaDataFor(oid, cacheName, candidates) }, 1,
                                     EMPTY_HIGH_WATER_MARK);
  }

  private MetaDataReader createEvictionMetaDataFor(ObjectID oid, String cacheName, Map candidates) {
    return new ServerMapEvictionMetaDataReader(oid, cacheName, candidates);
  }

  private DNA createServerMapEvictionDNAFor(final ObjectID oid, final String className, final String loaderDesc,
                                            final Map candidates, String cacheName) {
    return new ServerMapEvictionDNA(oid, className, loaderDesc, candidates, cacheName);
  }

  public ServerTransactionImpl createMemcacheRootTxn(NodeID localNodeID, long oid, String rootName) {
    Map rootMap = new HashMap();
    rootMap.put(rootName, new ObjectID(oid));
    return new ServerTransactionImpl(TxnBatchID.NULL_BATCH_ID, getNextTransactionID(), SequenceID.NULL_ID,
                                     NULL_LOCK_ID, localNodeID,
                                     Collections.singletonList(new MemcacheRootDNA(new ObjectID(oid))),
                                     new ObjectStringSerializerImpl(), rootMap, TxnType.NORMAL, Collections.EMPTY_LIST,
                                     NULL_DMI_DESCRIPTOR, new MetaDataReader[] {}, 1, EMPTY_HIGH_WATER_MARK);

  }

  public ServerTransactionImpl createMemcacheElementTxn(NodeID localNodeID, List changes) {
    return new ServerTransactionImpl(TxnBatchID.NULL_BATCH_ID, getNextTransactionID(), SequenceID.NULL_ID,
                                     NULL_LOCK_ID, localNodeID, changes, new ObjectStringSerializerImpl(),
                                     Collections.EMPTY_MAP, TxnType.NORMAL, Collections.EMPTY_LIST,
                                     NULL_DMI_DESCRIPTOR, new MetaDataReader[] {}, changes.size(),
                                     EMPTY_HIGH_WATER_MARK);

  }
}