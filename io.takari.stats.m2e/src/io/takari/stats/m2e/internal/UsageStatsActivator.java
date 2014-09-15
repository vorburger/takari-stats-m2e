package io.takari.stats.m2e.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;


/**
 * Weekly usage statistics reporter.
 * <p>
 * Usages is reported weekly and report is delayed {@link #REPORT_MINIMAL_DELAY} milliseconds from
 * bundle activation.
 * <p>
 * To disable usage reporting, this bundle needs to be stopped or remove from eclipse installation.
 * 
 * @see UsageStatsStartupHook
 */
public class UsageStatsActivator implements BundleActivator {

  private static final String BUNDLE_ID = "io.takari.stats.client.m2e";

  /** explicitly allows usage statistics collection (in other words, suppresses info popup) */
  private static final String SYSPREF_STATSALLOW = "eclipse.m2.stats.allow";

  private static final String PREF_INSTANCEID = "eclipse.m2.stats.instanceId";

  private static final String PREF_NEXTREPORT = "eclipse.m2.stats.nextReport";

  private static final long REPORT_MINIMAL_DELAY = TimeUnit.MINUTES.toMillis(10);

  private static final long REPORT_PERIOD = TimeUnit.DAYS.toMillis(7);

  private static final String PROTOCOL = "http";

  private static final String REPORT_URL = PROTOCOL + "://stats.takari.io/stats";

  /**
   * Presence of this bundle entry makes bundle symbolic name and version appear in the usage
   * report. Only active bundles are reported however.
   */
  public static final String MAGIC_BUNDLE_ENTRY = ".takaristats";

  private static final Logger log;

  private static final HttpClient HTTP;

  static {
    log = LoggerFactory.getLogger(UsageStatsActivator.class);

    HttpClient http = null;
    try {
      http = newHttpClient("io.takari.stats.m2e.internal.fragment16.HttpClient16");
    } catch (LinkageError | ReflectiveOperationException e16) {
      try {
        http = newHttpClient("io.takari.stats.m2e.internal.fragment15.HttpClient15");
      } catch (LinkageError | ReflectiveOperationException e15) {
        log.error("Could not instantiate m2e 1.6 http connector", e16);
        log.error("Could not instantiate m2e 1.5 http connector", e15);
      }
    }
    HTTP = http;
  }

  private static HttpClient newHttpClient(String impl) throws ReflectiveOperationException {
    return (HttpClient) UsageStatsActivator.class.getClassLoader().loadClass(impl).newInstance();
  }

  private static final Set<String> KNOWN_BUNDLES;

  static {
    Set<String> knownBundles = new HashSet<>();
    knownBundles.add("org.eclipse.osgi");
    knownBundles.add("org.eclipse.m2e.core");
    KNOWN_BUNDLES = Collections.unmodifiableSet(knownBundles);
  }

  private static UsageStatsActivator instance;

  private Timer timer;

  private IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(BUNDLE_ID);

  private BundleContext context;

  @Override
  public void start(BundleContext context) throws Exception {
    instance = this;
    this.context = context;

    if (prefs.get(PREF_INSTANCEID, null) == null) {
      initializeWorkspace();

      if (!Boolean.getBoolean(SYSPREF_STATSALLOW)) {
        // display info popup if not explicitly allowed to collect usage statistics
        final Display display = Display.getDefault();
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            new UsageStatsDialog(display.getActiveShell()).open();
          }
        };
        display.asyncExec(runnable);
      }
    }

    long initialDelay = prefs.getLong(PREF_NEXTREPORT, 0) - System.currentTimeMillis();
    if (initialDelay < REPORT_MINIMAL_DELAY) {
      initialDelay = REPORT_MINIMAL_DELAY;
    }
    initialDelay = 0;

    final TimerTask timerTask = new TimerTask() {
      @Override
      public void run() {
        reportUsageStats(REPORT_URL);
      }
    };

    // daemon timer won't prevent JVM from shutting down.
    timer = new Timer("Takari usage stats reporter", true);
    timer.scheduleAtFixedRate(timerTask, initialDelay, REPORT_PERIOD);
  }

  /**
   * initialize workspace-uuid and next report time
   */
  protected void initializeWorkspace() {
    prefs.put(PREF_INSTANCEID, UUID.randomUUID().toString());
    prefs.putLong(PREF_NEXTREPORT, System.currentTimeMillis() + REPORT_PERIOD);
    flushPreferences();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    timer.cancel();
    timer = null;
    context = null;
    instance = null;
  }

  public static UsageStatsActivator getInstance() {
    return instance;
  }

  public void reportUsageStats(String url) {
    final String instanceId = prefs.get(PREF_INSTANCEID, null);
    if (instanceId == null) {
      log.error("Could not report usage stats, workspace UUID is not initialized");
      return;
    }

    // update next report time regardless if we report or not
    prefs.putLong(PREF_NEXTREPORT, System.currentTimeMillis() + REPORT_PERIOD);
    flushPreferences();

    final IMavenProjectFacade[] projects = MavenPlugin.getMavenProjectRegistry().getProjects();
    final int projectCount = projects.length;
    if (projectCount > 0) {
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("java.version", System.getProperty("java.version", "unknown"));
      params.put("java.vendor", System.getProperty("java.vendor", "unknown"));
      params.put("os.name", System.getProperty("os.name", "unknown"));
      params.put("os.arch", System.getProperty("os.arch", "unknown"));
      params.put("os.version", System.getProperty("os.version", "unknown"));
      params.put("eclipse.product", System.getProperty("eclipse.product", "unknown"));
      params.put("eclipse.buildId", System.getProperty("eclipse.buildId", "unknown"));
      params.put("projectCount", projectCount);
      params.put("bundles", getBundles());
      params.put("mavenPlugins", getMavenPlugins(projects));

      post(url, toJson(params));
    }
  }

  private String toJson(Map<String, Object> data) {
    return new Gson().toJson(data);
  }

  private List<Map<String, String>> getBundles() {
    // TODO ideally, report all plugins activated at least once since previous report
    List<Map<String, String>> bundles = new ArrayList<>();
    for (Bundle bundle : context.getBundles()) {
      if ((bundle.getState() & Bundle.ACTIVE) > 0) {
        String bundleId = bundle.getSymbolicName();
        if (KNOWN_BUNDLES.contains(bundleId) || isReportingRequested(bundle)) {
          Map<String, String> info = new LinkedHashMap<String, String>();
          info.put("symbolicName", bundleId);
          info.put("version", bundle.getVersion().toString());
          bundles.add(info);
        }
      }
    }
    return bundles;
  }

  private Map<String, Set<String>> getMavenPlugins(IMavenProjectFacade[] projects) {
    Map<String, Set<String>> mavenPlugins = new TreeMap<>();
    for (IMavenProjectFacade project : projects) {
      for (Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : project
          .getMojoExecutionMapping().entrySet()) {
        if (!isKnownMavenPluginGroupId(entry.getKey())) {
          continue;
        }
        String mojoKey = toString(entry.getKey());
        Set<String> mojoMappings = mavenPlugins.get(mojoKey);
        if (mojoMappings == null) {
          mojoMappings = new TreeSet<>();
          mavenPlugins.put(mojoKey, mojoMappings);
        }
        for (IPluginExecutionMetadata mapping : entry.getValue()) {
          mojoMappings.add(mapping.getAction().toString());
        }
      }
    }
    return mavenPlugins;
  }

  private boolean isKnownMavenPluginGroupId(MojoExecutionKey key) {
    String groupId = key.getGroupId();
    return groupId.startsWith("org.apache") || groupId.startsWith("org.codehaus")
        || groupId.startsWith("io.takari");
  }

  private String toString(MojoExecutionKey key) {
    StringBuilder sb = new StringBuilder();
    sb.append(key.getGroupId());
    sb.append(':');
    sb.append(key.getArtifactId());
    sb.append(':');
    sb.append(key.getVersion());
    sb.append(':');
    sb.append(key.getGoal());
    return sb.toString();
  }

  private boolean isReportingRequested(Bundle bundle) {
    return bundle.getEntry(".takaristats") != null;
  }

  void flushPreferences() {
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      log.debug("Could not update preferences", e);
    }
  }

  private void post(String url, String body) {
    if (HTTP != null) {
      log.info("Reporting usage stats url={}, body={}", url, body);

      ProxyInfo proxyInfo = null;
      try {
        proxyInfo = MavenPlugin.getMaven().getProxyInfo(PROTOCOL);
      } catch (CoreException e) {
        log.debug("Could not read http proxy configuration", e);
      }
      if (proxyInfo != null && ProxyUtils.validateNonProxyHosts(proxyInfo, PathUtils.host(url))) {
        proxyInfo = null;
      }

      HTTP.post(url, body, proxyInfo);
    } else {
      log.error("Could not report usage stats, see previous http connection instantiation error");
    }
  }

  public static String getId() {
    Bundle bundle = instance.context.getBundle();
    return bundle.getSymbolicName() + "_" + bundle.getVersion().toString();
  }
}
