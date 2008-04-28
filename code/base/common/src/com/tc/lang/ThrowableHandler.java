/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.exception.DatabaseException;
import com.tc.exception.ExceptionHelperImpl;
import com.tc.exception.MortbayMultiExceptionHelper;
import com.tc.exception.RuntimeExceptionHelper;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.ThreadDumpHandler;
import com.tc.util.TCDataFileLockingException;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.net.BindException;
import java.util.Iterator;

/**
 * Handle throwables appropriately by printing messages to the logger, etc. Deal with nasty problems that can occur as
 * the VM shuts down.
 */
public class ThrowableHandler {
  // XXX: The dispatching in this class is retarded, but I wanted to move as much of the exception handling into a
  // single
  // place first, then come up with fancy ways of dealing with them. --Orion 03/20/2006

  private final TCLogger            logger;
  private final ExceptionHelperImpl helper;
  private CopyOnWriteArrayList      callbackOnExitHandlers = new CopyOnWriteArrayList();

  /**
   * Construct a new ThrowableHandler with a logger
   *
   * @param logger Logger
   */
  public ThrowableHandler(TCLogger logger) {
    this.logger = logger;
    helper = new ExceptionHelperImpl();
    helper.addHelper(new RuntimeExceptionHelper());
    helper.addHelper(new MortbayMultiExceptionHelper());
    registerDefaultCallbackHandlers();
   }

  public void addCallbackOnExitHandler(CallbackOnExitHandler callbackOnExitHandler) {
    callbackOnExitHandlers.add(callbackOnExitHandler);
  }


  /**
   * Handle throwable occurring on thread
   *
   * @param thread Thread receiving Throwable
   * @param t Throwable
   */
  public void handleThrowable(final Thread thread, final Throwable t) {
    final Throwable proximateCause = helper.getProximateCause(t);
    final Throwable ultimateCause = helper.getUltimateCause(t);
    if (proximateCause instanceof ConfigurationSetupException) {
      handleStartupException((ConfigurationSetupException) proximateCause);
    } else if (ultimateCause instanceof BindException) {
      logger.error(ultimateCause);
      handleStartupException((Exception) ultimateCause,
                             ".  Please make sure the server isn't already running or choose a different port.");
    } else if (ultimateCause instanceof DatabaseException) {
      handleStartupException((Exception) proximateCause);
    } else if (ultimateCause instanceof LocationNotCreatedException) {
      handleStartupException((Exception) ultimateCause);
    } else if (ultimateCause instanceof FileNotCreatedException) {
      handleStartupException((Exception) ultimateCause);
    } else if (ultimateCause instanceof TCDataFileLockingException) {
      handleStartupException((Exception) ultimateCause);
    } else {
      handleDefaultException(thread, proximateCause);
    }
  }

  protected void registerDefaultCallbackHandlers() {
    callbackOnExitHandlers.add( new ThreadDumpHandler());
  }

  protected void exit(int status) {
    System.exit(status);
  }

  private void handleDefaultException(Thread thread, Throwable throwable) {

    // We need to make SURE that our stacktrace gets printed, when using just the logger sometimes the VM exits
    // before the stacktrace prints
    try {
      throwable.printStackTrace(System.err);
      System.err.flush();

      logger.error("Thread:" + thread + " got an uncaught exception. calling CallbackOnExitHandlers.", throwable);

      try {
      for (Iterator iter = callbackOnExitHandlers.iterator(); iter.hasNext();) {
        CallbackOnExitHandler callbackOnExitHandler = (CallbackOnExitHandler) iter.next();
        callbackOnExitHandler.callbackOnExit();
      }
      } catch (Throwable t) {
        logger.error("Exception thrown in callbackOnExitHanders ", t);
      }

      logger.error("Thread:" + thread + " got an uncaught exception.  About to sleep then exit.", throwable);

      try {
        // Give our logger a chance to print the stacktrace before the VM exits
        Thread.sleep(3000);
      } catch (InterruptedException ie) {
        // When you suck you just suck and nothing will help you
      }
    } catch (Throwable t) {
      logger.error(t);
    } finally {
      exit(1);
    }
  }

  private void handleStartupException(Exception e) {
    handleStartupException(e, "");
  }

  private void handleStartupException(Exception e, String extraMessage) {
    System.err.println("");
    System.err.println("");
    System.err.println("Fatal Terracotta startup exception:");
    System.err.println("");
    System.err.println("   " + e.getMessage() + extraMessage);
    System.err.println("");
    System.err.println("Server startup failed.");
    exit(2);
  }

}
