/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DistributedMethodSpec;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;

/**
 * @author Eugene Kuleshov
 */
public class AnnotationTestApp extends AbstractTransparentApp {

  private final ClassWithAnnotations value = new ClassWithAnnotations();
  private final CyclicBarrier        mainBarrier;

  public AnnotationTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    mainBarrier = new CyclicBarrier(AnnotationTest.NODE_COUNT);
  }

  public void run() {
    try {
      testPortableOnAnnotation();
      testLockOnAnnotation();
      testDmiOnAnnotation();
      testRoot();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void testRoot() {
    ClassWithAnnotatedRoot o = new ClassWithAnnotatedRoot(getParticipantCount());
    o.await();
  }

  private void testPortableOnAnnotation() throws InterruptedException, BrokenBarrierException {
    mainBarrier.await();
    ClassIncludedWithAnnotation o = new ClassIncludedWithAnnotation("foo");
    value.setDistributed(o);
  }

  // CDV-271: Annotation support for Locks, includes by matching on what ever annotations they want.
  private void testLockOnAnnotation() throws InterruptedException, BrokenBarrierException {
    mainBarrier.await();

    value.setDistributed("foo");
    junit.framework.Assert.assertEquals("foo", value.getDistributed());
  }

  private void testDmiOnAnnotation() throws InterruptedException, BrokenBarrierException {
    mainBarrier.await();

    value.processMessage("msg from " + getApplicationId());

    mainBarrier.await();

    try {
      Thread.sleep(1000L * 10);
    } catch (InterruptedException ex) {
      // ignore
    }

    List<String> messages = value.getMessages();
    Assert.assertEquals(getParticipantCount(), messages.size());
    Assert.assertEquals(getParticipantCount(), new HashSet<String>(messages).size());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(AnnotationTestApp.class.getName());
    spec.addRoot("value", "value");
    spec.addRoot("mainBarrier", "mainBarrier");

    String testClass = "com.tctest.jdk15.AnnotationTestApp$ClassWithAnnotations";
    // config.addIncludePattern(testClass);

    config.addIncludePattern(ClassWithAnnotatedRoot.class.getName());

    config.addIncludePattern("@com.tctest.jdk15.AnnotationTestApp$Portable *", true);

    config.addWriteAutolock("@com.tctest.jdk15.AnnotationTestApp$WriteAutolock * " + testClass + ".*(..)");
    config.addReadAutolock("@com.tctest.jdk15.AnnotationTestApp$ReadAutolock * " + testClass + ".*(..)");

    config.addDistributedMethodCall(new DistributedMethodSpec("@com.tctest.jdk15.AnnotationTestApp$DistributedCall * "
                                                              + testClass + ".*(..)", true));

    String rootExpr = "@" + Root.class.getName() + " * *";
    com.tc.object.config.Root root = new com.tc.object.config.Root(rootExpr);
    config.addRoot(root, false);
  }

  @Portable
  public static class ClassWithAnnotations {
    private Object                 distributed;

    private transient List<String> messages = new ArrayList<String>();

    @ReadAutolock
    public Object getDistributed() {
      synchronized (this) {
        return distributed;
      }
    }

    @WriteAutolock
    public void setDistributed(Object distributed) {
      synchronized (this) {
        this.distributed = distributed;
      }
    }

    @DistributedCall
    public void processMessage(String message) {
      if (messages == null) {
        messages = new ArrayList<String>();
      }
      messages.add(message);
    }

    public List<String> getMessages() {
      return messages;
    }

  }

  @Portable
  public static class ClassIncludedWithAnnotation {
    private final String value;

    public ClassIncludedWithAnnotation(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static class ClassWithAnnotatedRoot {
    @Root
    private final CyclicBarrier barrier;

    public ClassWithAnnotatedRoot(int num) {
      if (num < 2) { throw new AssertionError(); }
      barrier = new CyclicBarrier(num);
    }

    public int await() {
      // this method will hang if the barrier does not become a root

      try {
        return barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Portable {
    //
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface WriteAutolock {
    //
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface ReadAutolock {
    //
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface DistributedCall {
    //
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Root {
    //
  }

}
