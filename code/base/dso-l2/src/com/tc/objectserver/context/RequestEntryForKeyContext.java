/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.msg.ServerTCMapRequestMessage;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collection;
import java.util.Map;

public final class RequestEntryForKeyContext implements ObjectManagerResultsContext {

  private final static TCLogger logger    = TCLogging.getLogger(RequestEntryForKeyContext.class);

  private final ObjectIDSet     lookupIDs = new ObjectIDSet();
  private final ObjectID        mapID;
  private final Object          portableKey;
  private final ClientID        clientID;
  private final Sink            destinationSink;

  public RequestEntryForKeyContext(final ServerTCMapRequestMessage mesg, final Sink destinationSink) {
    this.destinationSink = destinationSink;
    this.mapID = mesg.getMapID();
    this.portableKey = mesg.getPortableKey();
    this.clientID = mesg.getClientID();
    this.lookupIDs.add(this.mapID);
  }

  public ClientID getClientID() {
    return this.clientID;
  }

  public Object getPortableKey() {
    return this.portableKey;
  }

  public ObjectID getServerTCMapID() {
    return this.mapID;
  }

  @Override
  public String toString() {
    return "RequestEntryForKeyContext [  mapID = " + this.mapID + " key : " + this.portableKey + " clientID : "
           + this.clientID + "]";
  }

  public ObjectIDSet getLookupIDs() {
    return this.lookupIDs;
  }

  public ObjectIDSet getNewObjectIDs() {
    return TCCollections.EMPTY_OBJECT_ID_SET;
  }

  public void setResults(final ObjectManagerLookupResults results) {
    final Map<ObjectID, ManagedObject> objects = results.getObjects();
    final ObjectIDSet missingObjects = results.getMissingObjectIDs();
    final ObjectIDSet pendingLookups = results.getLookupPendingObjectIDs();

    if (!missingObjects.isEmpty()) {
      logger.error("Ignoring Missing ObjectIDs : " + missingObjects + " Map ID : " + this.mapID + " portable key : "
                   + this.portableKey);
      // TODO:: Fix this
      return;
    }
    if (objects.size() != 1) { throw new AssertionError("Asked for 1, got more or less"); }

    final Collection<ManagedObject> mobjs = objects.values();
    final ManagedObject mo = mobjs.iterator().next();

    if (!mo.getID().equals(this.mapID)) { throw new AssertionError("ServerTCMap (mapID " + this.mapID
                                                                   + " ) is not looked up "); }

    final EntryForKeyResponseContext responseContext = new EntryForKeyResponseContext(this.clientID, mo,
                                                                                      pendingLookups, this.mapID,
                                                                                      this.portableKey);
    this.destinationSink.add(responseContext);
  }

  public boolean updateStats() {
    return true;
  }
}