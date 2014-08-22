/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package io.takari.stats.client.m2e.internal;

import org.eclipse.m2e.core.internal.embedder.IMavenComponentContributor;


/**
 * This class is registered with m2e core as {@code mavenComponentContributor} extension and provides a (hackish) way to
 * trigger activation of this bundle whenever m2e core bundle is activated. Actual usage statistics reporting logic is
 * implemented in {@link UsageStatsActivator}.
 */
@SuppressWarnings("restriction")
public class UsageStatsStartupHook implements IMavenComponentContributor {

  @Override
  public void contribute(IMavenComponentBinder binder) {
  }

}
