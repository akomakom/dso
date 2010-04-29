/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.RemoteServerMapManager;
import com.tc.object.ServerMapRequestType;
import com.tc.object.msg.GetSizeServerMapResponseMessage;
import com.tc.object.msg.GetValueServerMapResponseMessage;
import com.tc.object.msg.ServerMapResponseMessage;

public class ReceiveServerMapResponseHandler extends AbstractEventHandler {

  private final RemoteServerMapManager remoteServerMapManager;

  public ReceiveServerMapResponseHandler(final RemoteServerMapManager remoteServerMapManager) {
    this.remoteServerMapManager = remoteServerMapManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ServerMapResponseMessage responseMsg = (ServerMapResponseMessage) context;
    final ServerMapRequestType requestType = responseMsg.getRequestType();
    switch (requestType) {
      case GET_SIZE:
        this.remoteServerMapManager.addResponseForGetSize(responseMsg.getLocalSessionID(), responseMsg.getMapID(),
                                                          responseMsg.getRequestID(),
                                                          ((GetSizeServerMapResponseMessage) responseMsg).getSize(),
                                                          responseMsg.getSourceNodeID());
        break;
      case GET_VALUE_FOR_KEY:
        this.remoteServerMapManager.addResponseForKeyValueMapping(responseMsg.getLocalSessionID(), responseMsg
            .getMapID(), responseMsg.getRequestID(), ((GetValueServerMapResponseMessage) responseMsg)
            .getPortableValue(), responseMsg.getSourceNodeID());
        break;
      default:
        throw new AssertionError("Unsupported Request type : " + requestType);
    }
  }
}
