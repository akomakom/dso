/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullReferenceTestApp extends AbstractErrorCatchingTransparentApp {

  private final CyclicBarrier barrier;
  private final Holder        holder = new Holder();

  public NullReferenceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NullReferenceTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrierLock");
    spec.addRoot("holder", "holderLock");

    String methodExpression;

    methodExpression = "* " + testClass + ".init()";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + ".check()";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + testClass + "$Holder.*(..)";
    config.addWriteAutolock(methodExpression);

    config.addIncludePattern(Holder.class.getName());
  }

  @Override
  protected void runTest() throws Throwable {
    barrier.await();

    holder.check();

    barrier.await();

    holder.mod();

    barrier.await();
  }

  private static class Holder {
    Object      reference = null;
    Object[]    array     = new Object[] { null, new Object(), null };

    Map         map1      = new HashMap();
    Map         map2      = new IdentityHashMap();

    List        list1     = new ArrayList();

    Set         set1      = new HashSet();

    private int count     = 0;

    Holder() {
      mod();
    }

    void check() {
      Assert.assertNull(reference);
      Assert.assertNull(array[0]);
      Assert.assertNotNull(array[1]);
      Assert.assertNull(array[2]);

      Assert.assertEquals(1, list1.size());
      Assert.assertTrue(list1.contains(null));

      Assert.assertEquals(2, map1.size());
      Assert.assertEquals(2, map2.size());
      Assert.assertTrue(map1.containsKey(null));
      Assert.assertTrue(map2.containsKey(null));
      Assert.assertTrue(map1.containsValue(null));
      Assert.assertTrue(map2.containsValue(null));

      Assert.assertEquals(1, set1.size());
      Assert.assertTrue(set1.contains(null));
    }

    synchronized void mod() {
      modSets(new Set[] { set1 });
      modLists(new List[] { list1 });
      modMaps(new Map[] { map1, map2 });
      reference = null;
      array[0] = null;
    }

    void modLists(List[] lists) {
      for (List list : lists) {
        list.add(null);
      }
    }

    void modSets(Set[] sets) {
      for (Set set : sets) {
        set.add(null);
      }
    }

    void modMaps(Map[] maps) {
      for (Map map : maps) {
        map.put(null, "value for null key");
        map.put("key" + count + " for null value", null);
      }
      count++;
    }

  }

}
