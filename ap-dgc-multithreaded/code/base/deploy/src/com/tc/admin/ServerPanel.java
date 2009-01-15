/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.PropertyTable;
import com.tc.admin.common.PropertyTableModel;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

public class ServerPanel extends XContainer {
  protected ApplicationContext appContext;
  protected IServer            server;
  protected ServerListener     serverListener;

  private XTabbedPane          tabbedPane;
  private StatusView           statusView;
  protected XContainer         controlArea;
  private XContainer           restartInfoItem;
  private PropertyTable        propertyTable;

  private XTextArea            environmentTextArea;
  private XTextArea            configTextArea;
  private ServerLoggingPanel   loggingPanel;

  public ServerPanel(ApplicationContext appContext, IServer server) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.server = server;

    tabbedPane = new XTabbedPane();

    /** Main **/
    XContainer mainPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.NORTH;

    XContainer topPanel = new XContainer(new GridBagLayout());
    topPanel.add(statusView = new StatusView(), gbc);
    statusView.setText("Not connected");
    gbc.gridx++;

    topPanel.add(controlArea = new XContainer(), gbc);
    gbc.gridx++;

    // topPanel filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);

    gbc.gridx = gbc.gridy = 0;
    mainPanel.add(topPanel, gbc);
    gbc.gridy++;
    gbc.weightx = 1.0;

    mainPanel.add(restartInfoItem = new XContainer(new BorderLayout()), gbc);
    gbc.gridy++;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;

    propertyTable = new PropertyTable();
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
    propertyTable.setDefaultRenderer(Long.class, renderer);
    propertyTable.setDefaultRenderer(Integer.class, renderer);
    mainPanel.add(new XScrollPane(propertyTable), gbc);

    XContainer mainPanelHolder = new XContainer(new BorderLayout());
    mainPanelHolder.add(mainPanel, BorderLayout.NORTH);

    tabbedPane.addTab("Main", mainPanelHolder);

    /** Environment **/
    XContainer envPanel = new XContainer(new BorderLayout());
    environmentTextArea = new XTextArea();
    environmentTextArea.setEditable(false);
    environmentTextArea.setFont(new Font("monospaced", Font.PLAIN, 12));
    envPanel.add(new XScrollPane(environmentTextArea));
    envPanel.add(new SearchPanel(appContext, environmentTextArea), BorderLayout.SOUTH);
    tabbedPane.addTab("Environment", envPanel);

    /** Config **/
    XContainer configPanel = new XContainer(new BorderLayout());
    configTextArea = new XTextArea();
    configTextArea.setEditable(false);
    configTextArea.setFont(new Font("monospaced", Font.PLAIN, 12));
    configPanel.add(new XScrollPane(configTextArea));
    configPanel.add(new SearchPanel(appContext, configTextArea), BorderLayout.SOUTH);
    tabbedPane.addTab("Config", configPanel);

    /** Logging **/
    loggingPanel = createLoggingPanel(appContext, server);
    if (loggingPanel != null) {
      tabbedPane.addTab("Logging", loggingPanel);
    }

    hideInfoContent();

    add(tabbedPane, BorderLayout.CENTER);

    serverListener = new ServerListener(server);
    serverListener.startListening();
  }

  synchronized IServer getServer() {
    return server;
  }

  synchronized ApplicationContext getApplicationContext() {
    return appContext;
  }

  protected ServerLoggingPanel createLoggingPanel(ApplicationContext theAppContext, IServer theServer) {
    return new ServerLoggingPanel(theAppContext, theServer);
  }

  protected class ServerListener extends AbstractServerListener {
    public ServerListener(IServer server) {
      super(server);
    }

    protected void handleConnectError() {
      IServer theServer = getServer();
      if (theServer != null) {
        Exception e = theServer.getConnectError();
        String msg = theServer.getConnectErrorMessage(e);
        if (msg != null) {
          setConnectExceptionMessage(msg);
        }
      }
    }

    /**
     * The only differences between activated() and started() is the status message and the serverlog is only added in
     * activated() under the presumption that a non-active server won't be saying anything.
     */

    protected void handleStarting() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new StartedWorker());
      }
    }

    protected void handleActivation() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new ActivatedWorker());
      }
    }

    protected void handlePassiveStandby() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new PassiveStandbyWorker());
      }
    }

    protected void handlePassiveUninitialized() {
      ApplicationContext theAppContext = getApplicationContext();
      if (theAppContext != null) {
        theAppContext.execute(new PassiveUninitializedWorker());
      }
    }

    protected void handleDisconnected() {
      disconnected();
    }

  }

  protected void storePreferences() {
    ApplicationContext theAppContext = getApplicationContext();
    if (theAppContext != null) {
      theAppContext.storePrefs();
    }
  }

  private static class ServerState {
    private Date    fStartDate;
    private Date    fActivateDate;
    private String  fVersion;
    private String  fPatchLevel;
    private String  fCopyright;
    private String  fPersistenceMode;
    private String  fEnvironment;
    private String  fConfig;
    private Integer fDSOListenPort;

    ServerState(Date startDate, Date activateDate, String version, String patchLevel, String copyright,
                String persistenceMode, String environment, String config, Integer dsoListenPort) {
      fStartDate = startDate;
      fActivateDate = activateDate;
      fVersion = version;
      fPatchLevel = patchLevel;
      fCopyright = copyright;
      fPersistenceMode = persistenceMode;
      fEnvironment = environment;
      fConfig = config;
      fDSOListenPort = dsoListenPort;
    }

    Date getStartDate() {
      return fStartDate;
    }

    Date getActivateDate() {
      return fActivateDate;
    }

    String getVersion() {
      return fVersion;
    }

    String getPatchLevel() {
      return fPatchLevel;
    }

    String getCopyright() {
      return fCopyright;
    }

    String getPersistenceMode() {
      return fPersistenceMode;
    }

    String getEnvironment() {
      return fEnvironment;
    }

    String getConfig() {
      return fConfig;
    }

    Integer getDSOListenPort() {
      return fDSOListenPort;
    }
  }

  /**
   * TODO: grab all of these in one-shot.
   */
  private class ServerStateWorker extends BasicWorker<ServerState> {
    private ServerStateWorker() {
      super(new Callable<ServerState>() {
        public ServerState call() throws Exception {
          IServer theServer = getServer();
          if (theServer == null) throw new IllegalStateException("not connected");
          Date startDate = new Date(theServer.getStartTime());
          Date activateDate = new Date(theServer.getActivateTime());
          String version = theServer.getProductVersion();
          String patchLevel = theServer.getProductPatchLevel();
          String copyright = theServer.getProductCopyright();
          String persistenceMode = theServer.getPersistenceMode();
          String environment = theServer.getEnvironment();
          String config = theServer.getConfig();
          Integer dsoListenPort = theServer.getDSOListenPort();

          return new ServerState(startDate, activateDate, version, patchLevel, copyright, persistenceMode, environment,
                                 config, dsoListenPort);
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          ApplicationContext theAppContext = getApplicationContext();
          if (theAppContext != null) {
            theAppContext.log(e);
          }
        }
      } else {
        if (!tabbedPane.isEnabled()) { // showInfoContent enables tabbedPane
          ServerState serverState = getResult();
          showInfoContent();
          environmentTextArea.setText(serverState.getEnvironment());
          configTextArea.setText(serverState.getConfig());
          if (loggingPanel != null) {
            loggingPanel.setupLoggingControls();
          }
        }
      }
    }
  }

  private class StartedWorker extends ServerStateWorker {
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        ServerState serverState = getResult();
        String startTime = serverState.getStartDate().toString();
        setStatusLabel(appContext.format("server.started.label", startTime));
        appContext.setStatus(appContext.format("server.started.status", theServer, startTime));
      } else {
        appContext.log(getException());
      }
    }
  }

  private class ActivatedWorker extends ServerStateWorker {
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        ServerState serverState = getResult();
        String activateTime = serverState.getActivateDate().toString();
        setStatusLabel(appContext.format("server.activated.label", activateTime));
        appContext.setStatus(appContext.format("server.activated.status", theServer, activateTime));
      } else {
        appContext.log(getException());
      }
    }
  }

  private class PassiveUninitializedWorker extends ServerStateWorker {
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        String startTime = new Date().toString();
        setStatusLabel(appContext.format("server.initializing.label", startTime));
        appContext.setStatus(appContext.format("server.initializing.status", theServer, startTime));
      }
    }
  }

  private class PassiveStandbyWorker extends ServerStateWorker {
    protected void finished() {
      IServer theServer = getServer();
      if (theServer == null) return;
      super.finished();
      if (getException() == null) {
        String startTime = new Date().toString();
        setStatusLabel(appContext.format("server.standingby.label", startTime));
        appContext.setStatus(appContext.format("server.standingby.status", theServer, startTime));
      }
    }
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    if (appContext != null) {
      appContext.execute(new StartedWorker());
    }
  }

  void activated() {
    if (appContext != null) {
      appContext.execute(new ActivatedWorker());
    }
  }

  void passiveUninitialized() {
    if (appContext != null) {
      appContext.execute(new PassiveUninitializedWorker());
    }
  }

  void passiveStandby() {
    if (appContext != null) {
      appContext.execute(new PassiveStandbyWorker());
    }
  }

  private void testShowRestartInfoItem() {
    IServer theServer = getServer();
    if (theServer == null) return;
    if (!theServer.getPersistenceMode().equals("permanent-store")) {
      String warning = appContext.getString("server.non-restartable.warning");
      restartInfoItem.add(new PersistenceModeWarningPanel(appContext, warning));
    } else {
      restartInfoItem.removeAll();
    }
  }

  protected void showInfoContent() {
    testShowRestartInfoItem();
    showProductInfo();
  }

  protected void hideInfoContent() {
    hideProductInfo();
    hideRestartInfo();
  }

  private void hideRestartInfo() {
    restartInfoItem.removeAll();
    restartInfoItem.revalidate();
    restartInfoItem.repaint();
  }

  void disconnected() {
    IServer theServer = getServer();
    if (theServer == null) return;
    String startTime = new Date().toString();
    setStatusLabel(appContext.format("server.disconnected.label", startTime));
    appContext.setStatus(appContext.format("server.disconnected.status", theServer, startTime));
    hideInfoContent();
  }

  private void setTabbedPaneEnabled(boolean enabled) {
    tabbedPane.setEnabled(enabled);
    int tabCount = tabbedPane.getTabCount();
    for (int i = 0; i < tabCount; i++) {
      tabbedPane.setEnabledAt(i, enabled);
    }
    tabbedPane.setSelectedIndex(0);
  }

  void setConnectExceptionMessage(String msg) {
    setStatusLabel(msg);
    setTabbedPaneEnabled(false);
  }

  void setStatusLabel(String text) {
    IServer theServer = getServer();
    if (theServer == null) return;
    statusView.setText(text);
    statusView.setIndicator(ServerHelper.getHelper().getServerStatusColor(theServer));
    statusView.revalidate();
    statusView.repaint();
  }

  /**
   * The fields listed below are on IServer. If those methods change, so will these fields need to change. PropertyTable
   * uses reflection to access values to display. TODO: i18n
   */
  private void showProductInfo() {
    String[] fields = { "CanonicalHostName", "HostAddress", "Port", "DSOListenPort", "ProductVersion",
        "ProductBuildID", "ProductLicense", "PersistenceMode", "FailoverMode" };
    String[] headings = { "Host", "Address", "JMX port", "DSO port", "Version", "Build", "License", "Persistence mode",
        "Failover mode" };
    List<String> fieldList = new ArrayList(Arrays.asList(fields));
    List<String> headingList = new ArrayList(Arrays.asList(headings));
    String patch = server.getProductPatchLevel();
    if (patch != null && patch.length() > 0) {
      fieldList.add(fieldList.indexOf("ProductLicense"), "ProductPatchVersion");
      headingList.add(headingList.indexOf("License"), "Patch");
    }
    fields = fieldList.toArray(new String[fieldList.size()]);
    headings = headingList.toArray(new String[headingList.size()]);
    propertyTable.setModel(new PropertyTableModel(server, fields, headings));
    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, propertyTable);
    if (scrollPane != null) {
      scrollPane.setVisible(true);
    }

    setTabbedPaneEnabled(true);

    revalidate();
    repaint();
  }

  private void hideProductInfo() {
    propertyTable.setModel(new PropertyTableModel());
    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, propertyTable);
    if (scrollPane != null) {
      scrollPane.setVisible(false);
    }
    tabbedPane.setSelectedIndex(0);
    tabbedPane.setEnabled(false);

    revalidate();
    repaint();
  }

  public synchronized void tearDown() {
    statusView.tearDown();

    super.tearDown();

    appContext = null;
    server = null;
    propertyTable = null;
    statusView = null;
    tabbedPane = null;
    environmentTextArea = null;
    configTextArea = null;
    loggingPanel = null;
  }
}
