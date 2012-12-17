/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.L2Info;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.setup.ConfigurationSetupException;

import java.io.IOException;

public interface TCServer {
  String[] processArguments();

  void start() throws Exception;

  void stop();

  boolean isStarted();

  boolean isActive();

  boolean isStopped();

  long getStartTime();

  void updateActivateTime();

  long getActivateTime();

  boolean canShutdown();

  void shutdown();

  boolean isGarbageCollectionEnabled();

  int getGarbageCollectionInterval();

  String getConfig();

  boolean getRestartable();

  String getFailoverMode();

  String getDescriptionOfCapabilities();

  L2Info[] infoForAllL2s();

  ServerGroupInfo[] serverGroups();

  void startBeanShell(int port);

  int getDSOListenPort();

  int getDSOGroupPort();

  void waitUntilShutdown();

  void dump();

  void dumpClusterState();

  void reloadConfiguration() throws ConfigurationSetupException;

  boolean isProduction();

  boolean isSecure();

  String getSecurityServiceLocation();

  Integer getSecurityServiceTimeout();

  String getSecurityHostname();

  String getRunningBackup();

  String getBackupStatus(String name) throws IOException;

  void backup(String name) throws IOException;

  String getResourceState();
}
