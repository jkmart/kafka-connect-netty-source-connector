/**
 * Copyright  Vitalii Rudenskyi (vrudenskyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mckesson.kafka.connect.utils;

import okhttp3.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import org.apache.kafka.common.Configurable;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpClientConfig extends AbstractConfig {
  public static final String HTTP_CLIENT_PREFIX = "httpClient.";

  public static final String CONNECT_TIMEOUT_CONFIG = HTTP_CLIENT_PREFIX + "connectTimeout";
  public static final int CONNECT_TIMEOUT_DEFAULT = 30000;

  public static final String SOCKET_TIMEOUT_CONFIG = HTTP_CLIENT_PREFIX + "socketTimeout";
  public static final int SOCKET_TIMEOUT_DEFAULT = 30000;

  public static final String RETRY_ON_CONN_FAILURE_CONFIG = HTTP_CLIENT_PREFIX + "retryOnConnectionFailure";
  public static final Boolean RETRY_ON_CONN_FAILURE_DEFAULT = Boolean.TRUE;

  public static final String ENABLE_COOKIES_CONFIG = HTTP_CLIENT_PREFIX + "enableCookies";
  public static final Boolean ENABLE_COOKIES_DEFAULT = Boolean.FALSE;

  public static final String COOKIEJAR_CONFIG = HTTP_CLIENT_PREFIX + "cookiejar.";
  public static final String COOKIEJAR_CLASS_CONFIG = COOKIEJAR_CONFIG + "class";
  public static final Class COOKIEJAR_CLASS_DEFAULT = SimpleCookieJar.class;

  public static final String POOL_MAX_IDLE_CONNECTIONS_CONFIG = HTTP_CLIENT_PREFIX + "connectionPool.maxIdleConnections";
  public static final Integer POOL_MAX_IDLE_CONNECTIONS_DEFAULT = 5;

  public static final String POOL_KEEP_ALIVE_DURATION_CONFIG = HTTP_CLIENT_PREFIX + "connectionPool.keepAliveDuration";
  public static final Long POOL_KEEP_ALIVE_DURATION_DEFAULT = 5 * 60 * 1000L; //5 minutes

  public static final ConfigDef CONFIG = baseConfigDef();

  protected static ConfigDef baseConfigDef() {
    final ConfigDef configDef = new ConfigDef();
    addConfig(configDef);
    return configDef;
  }

  public static void addConfig(ConfigDef config) {

    config
        .define(CONNECT_TIMEOUT_CONFIG, Type.LONG, CONNECT_TIMEOUT_DEFAULT, null, Importance.MEDIUM, "Http Client Connect timeout Default: " + CONNECT_TIMEOUT_DEFAULT)
        .define(SOCKET_TIMEOUT_CONFIG, Type.LONG, SOCKET_TIMEOUT_DEFAULT, null, Importance.MEDIUM, "Http Client Socket timeout. Default: " + SOCKET_TIMEOUT_DEFAULT)
        .define(POOL_MAX_IDLE_CONNECTIONS_CONFIG, Type.INT, POOL_MAX_IDLE_CONNECTIONS_DEFAULT, null, Importance.MEDIUM, "Http Client Connection Pool Max idle connections. Default: " + POOL_MAX_IDLE_CONNECTIONS_DEFAULT)
        .define(POOL_KEEP_ALIVE_DURATION_CONFIG, Type.LONG, POOL_KEEP_ALIVE_DURATION_DEFAULT, null, Importance.MEDIUM, "Http Client Connection Pool Max idle connections. Default: " + POOL_KEEP_ALIVE_DURATION_DEFAULT)
        .define(RETRY_ON_CONN_FAILURE_CONFIG, Type.BOOLEAN, RETRY_ON_CONN_FAILURE_DEFAULT, null, Importance.LOW, "Retry on Conn failure. Default: " + RETRY_ON_CONN_FAILURE_DEFAULT)
        .define(ENABLE_COOKIES_CONFIG, Type.BOOLEAN, ENABLE_COOKIES_DEFAULT, null, Importance.LOW, "Enable cookies. Default: " + ENABLE_COOKIES_DEFAULT)
        .define(COOKIEJAR_CLASS_CONFIG, Type.CLASS, COOKIEJAR_CLASS_DEFAULT, null, Importance.LOW, "Default implementation for CookierJar. Default: " + ENABLE_COOKIES_DEFAULT)

        .withClientSslSupport();

  }

  private OkHttpClientConfig(Map<String, ?> props) {
    super(CONFIG, props);
  }

  public static OkHttpClient buildClient(Map<String, ?> props) throws Exception {
    return buildClient(props, Authenticator.NONE);
  }

  public static OkHttpClient.Builder builder(Map<String, ?> props) throws Exception {
    return builder(props, Authenticator.NONE);
  }

  public static OkHttpClient buildClient(Map<String, ?> props, Authenticator authenticator) throws Exception {
    return builder(props, authenticator).build();
  }

  public static OkHttpClient.Builder builder(Map<String, ?> props, Authenticator authenticator) throws Exception {

    OkHttpClientConfig conf = new OkHttpClientConfig(props);
    CookieJar cookieJar = conf.getBoolean(ENABLE_COOKIES_CONFIG) ? conf.getConfiguredInstance(COOKIEJAR_CLASS_CONFIG, CookieJar.class) : CookieJar.NO_COOKIES;

    if (cookieJar instanceof Configurable) {
      ((Configurable) cookieJar).configure(conf.originalsWithPrefix(COOKIEJAR_CONFIG));
    }
    OkHttpClient.Builder hcb = new OkHttpClient.Builder()
        .authenticator(authenticator)
        .connectionPool(new ConnectionPool(conf.getInt(POOL_MAX_IDLE_CONNECTIONS_CONFIG), conf.getLong(POOL_KEEP_ALIVE_DURATION_CONFIG), TimeUnit.MILLISECONDS))
        .connectTimeout(conf.getLong(CONNECT_TIMEOUT_CONFIG), TimeUnit.MILLISECONDS)
        .readTimeout(conf.getLong(SOCKET_TIMEOUT_CONFIG), TimeUnit.MILLISECONDS)
        .writeTimeout(conf.getLong(SOCKET_TIMEOUT_CONFIG), TimeUnit.MILLISECONDS)
        .cookieJar(cookieJar)
        .retryOnConnectionFailure(conf.getBoolean(RETRY_ON_CONN_FAILURE_CONFIG));

    // configure ssl/tls 
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(SslUtils.loadTrustStore(conf));
    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
      throw new IllegalStateException("Unexpected default trust managers:"
          + Arrays.toString(trustManagers));
    }
    X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
    SSLContext sslContext = SslUtils.createSSLContext(conf);
    hcb.sslSocketFactory(sslContext.getSocketFactory(), trustManager);

    return hcb;
  }

}
