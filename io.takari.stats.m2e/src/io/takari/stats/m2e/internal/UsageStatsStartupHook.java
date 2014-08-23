package io.takari.stats.m2e.internal;

import org.eclipse.m2e.core.internal.embedder.IMavenComponentContributor;


/**
 * This class is registered with m2e core as {@code mavenComponentContributor} extension and
 * provides a (hackish) way to trigger activation of this bundle whenever m2e core bundle is
 * activated. Actual usage statistics reporting logic is implemented in {@link UsageStatsActivator}.
 */
@SuppressWarnings("restriction")
public class UsageStatsStartupHook implements IMavenComponentContributor {

  @Override
  public void contribute(IMavenComponentBinder binder) {
  }

}
