/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.stats.m2e.internal;

import org.apache.maven.wagon.proxy.ProxyInfo;

/**
 * HTTP client interface.
 * <p>
 * Generic aether connector API is specific to artifact upload and download and connect be used to
 * upload data to arbitrary HTTP URLs. This is why we have to use low-level connector
 * implementation.
 * <p>
 * m2e 1.4.x and earlier used aether connector based on async-http-client and netty and is not
 * currently supported.
 * <p>
 * m2e 1.5 and 1.6 M1 use older version of okhttp aether connector which is not API-compatible with
 * okhttp aether connector used by m2e 1.6 M2 and newer. To compensate, corresponding
 * implementations are kept in their own fragments, but compiled results are copied to fragments/
 * folder and committed to git. This is yucky, but seems to be the easiest way to support both m2e
 * 1.5 and 1.6 and also provides flexibility to support newer connector versions in the future (if
 * history any indication, we'll be using another http client before too long).
 */
public interface HttpClient {

  void post(String body, String url, ProxyInfo proxyInfo);

}
