package io.takari.stats.client.m2e.internal;

import io.tesla.aether.client.AetherClientAuthentication;
import io.tesla.aether.client.AetherClientConfig;
import io.tesla.aether.client.AetherClientProxy;
import io.tesla.aether.client.RetryableSource;
import io.tesla.aether.okhttp.OkHttpAetherClient;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.Bundles;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
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
@SuppressWarnings("restriction")
public class UsageStatsActivator implements BundleActivator {

  private static final String BUNDLE_ID = "io.takari.stats.client.m2e";

  private static final String PREF_INSTANCEID = "eclipse.m2.stats.instanceId";

  private static final String PREF_NEXTREPORT = "eclipse.m2.stats.nextReport";

  private static final long REPORT_MINIMAL_DELAY = TimeUnit.MINUTES.toMillis(10);

  private static final long REPORT_PERIOD = TimeUnit.DAYS.toMillis(7);

  private static final String REPORT_URL = "https://stats.takari.io/stats";

  private static UsageStatsActivator instance;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private Timer timer;

  private IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(BUNDLE_ID);

  private Bundle bundle;

  @Override
  public void start(BundleContext context) throws Exception {
    instance = this;
    bundle = context.getBundle();

    // daemon timer won't prevent JVM from shutting down.
    timer = new Timer("Takari usage stats reporter", true);

    long initialDelay = prefs.getLong(PREF_NEXTREPORT, 0) - System.currentTimeMillis();
    if (initialDelay < REPORT_MINIMAL_DELAY) {
      initialDelay = REPORT_MINIMAL_DELAY;
    }

    TimerTask timerTask = new TimerTask() {
      @Override
      public void run() {
        reportUsageStats(REPORT_URL);
      }
    };

    timer.scheduleAtFixedRate(timerTask, initialDelay, REPORT_PERIOD);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    timer.cancel();
    timer = null;
    bundle = null;
    instance = null;
  }

  public static UsageStatsActivator getInstance() {
    return instance;
  }

  public void reportUsageStats(String url) {
    // update next report time regardless if we report or not
    prefs.putLong(PREF_NEXTREPORT, System.currentTimeMillis() + REPORT_PERIOD);
    flushPreferences();

    final int projectCount = MavenPlugin.getMavenProjectRegistry().getProjects().length;
    if (projectCount > 0) {
      String instanceId = prefs.get(PREF_INSTANCEID, null);
      if (instanceId == null) {
        instanceId = UUID.randomUUID().toString();
        prefs.put(PREF_INSTANCEID, instanceId);
        flushPreferences();
      }

      Map<String, Object> params = new LinkedHashMap<>();
      params.put("workspaceUUID", instanceId);
      params.put("projectCount", projectCount);
      params.put("m2e.version", MavenPluginActivator.getQualifiedVersion());
      params.put("equinox.version", getEquinoxVersion());
      params.put("java.version", System.getProperty("java.version", "unknown"));
      params.put("java.vendor", System.getProperty("java.vendor", "unknown"));
      params.put("os.name", System.getProperty("os.name", "unknown"));
      params.put("os.arch", System.getProperty("os.arch", "unknown"));
      params.put("os.version", System.getProperty("os.version", "unknown"));

      post(url, toJson(params));
    }
  }

  private String toJson(Map<String, Object> data) {
    return new Gson().toJson(data);
  }

  protected String getEquinoxVersion() {
    Bundle equinox = Bundles.findDependencyBundle(bundle, "org.eclipse.osgi");
    return equinox.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
  }

  void flushPreferences() {
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      log.debug("Could not update preferences", e);
    }
  }

  private void post(String url, String body) {
    log.info("Reporting usage stats url={}, body={}", url, body);

    AetherClientConfig config = new AetherClientConfig();
    config.setUserAgent(MavenPluginActivator.getUserAgent());
    config.setConnectionTimeout(15 * 1000); // XXX
    config.setRequestTimeout(60 * 1000); // XXX

    ProxyInfo proxyInfo = null;
    try {
      proxyInfo = MavenPlugin.getMaven().getProxyInfo("https");
    } catch (CoreException e) {
      log.debug("Could not read http proxy configuration", e);
    }
    if (proxyInfo != null && !ProxyUtils.validateNonProxyHosts(proxyInfo, PathUtils.host(url))) {
      AetherClientProxy proxy = new AetherClientProxy();
      proxy.setHost(proxyInfo.getHost());
      proxy.setPort(proxyInfo.getPort());
      proxy.setAuthentication(new AetherClientAuthentication(proxyInfo.getUserName(), proxyInfo
          .getPassword()));
      config.setProxy(proxy);
    }

    try {
      final byte[] bytes = body.getBytes("UTF-8");
      new OkHttpAetherClient(config).put(url, new RetryableSource() {
        @Override
        public long length() {
          return bytes.length;
        }

        @Override
        public void copyTo(OutputStream os) throws IOException {
          os.write(bytes);
        }
      });
    } catch (IOException e) {
      log.error("Could not submit usage statistics to {}", url, e);
    }
  }

}
