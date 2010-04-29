/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;

public interface GetSizeServerMapRequestMessage extends ServerMapRequestMessage {

  public void initializeGetSizeRequest(ServerMapRequestID requestID, ObjectID mapID);

}
