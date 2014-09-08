package io.takari.stats.m2e.internal.fragment15;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tesla.aether.client.AetherClientAuthentication;
import io.tesla.aether.client.AetherClientConfig;
import io.tesla.aether.client.AetherClientProxy;
import io.tesla.aether.client.Response;
import io.tesla.aether.client.RetryableSource;
import io.tesla.aether.okhttp.OkHttpAetherClient;
import io.takari.stats.m2e.internal.HttpClient;

@SuppressWarnings("restriction")
public class HttpClient15 implements HttpClient {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void post(String url, String body, ProxyInfo proxyInfo) {
    AetherClientConfig config = new AetherClientConfig();
    config.setUserAgent(MavenPluginActivator.getUserAgent());
    config.setConnectionTimeout(15 * 1000); // XXX
    config.setRequestTimeout(60 * 1000); // XXX

    if (proxyInfo != null) {
      AetherClientProxy proxy = new AetherClientProxy();
      proxy.setHost(proxyInfo.getHost());
      proxy.setPort(proxyInfo.getPort());
      proxy.setAuthentication(new AetherClientAuthentication(proxyInfo.getUserName(), proxyInfo
          .getPassword()));
      config.setProxy(proxy);
    }

    try {
      final byte[] bytes = body.getBytes("UTF-8");
      Response response = new OkHttpAetherClient(config).put(url, new RetryableSource() {
        @Override
        public long length() {
          return bytes.length;
        }

        @Override
        public void copyTo(OutputStream os) throws IOException {
          os.write(bytes);
        }
      });
      if (response.getStatusCode() > 299) {
        log.error("HTTP {}/{}", response.getStatusCode(), response.getStatusMessage());
      }
    } catch (IOException e) {
      log.error("Could not submit usage statistics to {}", url, e);
    }
  }

}
