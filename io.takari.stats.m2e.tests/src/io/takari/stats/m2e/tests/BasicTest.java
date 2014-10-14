/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.stats.m2e.tests;

import io.takari.stats.m2e.internal.UsageStatsActivator;

import java.util.List;

import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.common.HttpServer;

@SuppressWarnings("restriction")
public class BasicTest extends AbstractMavenProjectTestCase {

  private HttpServer server;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server = new HttpServer();
    server.enableRecording("/.*");
    server.start();
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
    super.tearDown();
  }

  public void testBasic() throws Exception {
    assertNotNull(importProject("projects/simple/pom.xml"));

    UsageStatsActivator reporter = UsageStatsActivator.getInstance();
    reporter.reportUsageStats(server.getHttpUrl());

    List<String> requests = server.getRecordedRequests();
    assertEquals(1, requests.size());
  }

}
