/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.test.TCTestCase;

public class ConnectionIDTest extends TCTestCase {

  private static final String VALID_SERVER_ID    = "aaBBccddeeff11223344556677889900";
  private static final String INVALID_SERVER_ID1 = "Gabbccddeeff11223344556677889900"; // bad char
  private static final String INVALID_SERVER_ID2 = "abbccddeeff11223344556677889900"; // bad length

  public void test() {
    try {
      ConnectionID connectionID = ConnectionID.parse("12." + VALID_SERVER_ID + ".jvm1");
      assertEquals(12, connectionID.getChannelID());
      assertEquals(VALID_SERVER_ID, connectionID.getServerID());
      assertEquals("jvm1", connectionID.getJvmID());
      assertEquals(false, connectionID.isJvmIDNull());
    } catch (InvalidConnectionIDException e) {
      fail(e);
    }

    try {
      ConnectionID.parse("");
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

    try {
      ConnectionID.parse(null);
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

    try {
      ConnectionID.parse("sdljksdf");
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

    try {
      ConnectionID.parse("." + VALID_SERVER_ID);
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

    try {
      ConnectionID.parse(VALID_SERVER_ID + ".");
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

    try {
      ConnectionID.parse(VALID_SERVER_ID + ".42");
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

    try {
      ConnectionID.parse("212." + INVALID_SERVER_ID1);
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

    try {
      ConnectionID.parse("144." + INVALID_SERVER_ID2);
      fail();
    } catch (InvalidConnectionIDException e) {
      // expected
    }

  }

}