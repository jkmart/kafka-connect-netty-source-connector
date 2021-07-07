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

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;

import java.util.Map;

public class HttpClientConfig extends AbstractConfig {

  public static final String HTTP_CLIENT_PREFIX = "httpClient.";

  public static final String CONNECT_TIMEOUT_CONFIG = HTTP_CLIENT_PREFIX + "connectTimeout";
  public static final int CONNECT_TIMEOUT_DEFAULT = 30000;

  public static final String SOCKET_TIMEOUT_CONFIG = HTTP_CLIENT_PREFIX + "socketTimeout";
  public static final int SOCKET_TIMEOUT_DEFAULT = 30000;

  public static final String CONNECTION_REQUEST_TIMEOUT_CONFIG = HTTP_CLIENT_PREFIX + "connectionRequestTimeout";
  public static final int CONNECTION_REQUEST_TIMEOUT_DEFAULT = 30000;

  public static final String RETRY_COUNT_CONFIG = HTTP_CLIENT_PREFIX + "retryCount";
  public static final int RETRY_COUNT_DEFAULT = 3;

  public static final String REQUEST_SENT_RETRY_ENABLED_CONFIG = HTTP_CLIENT_PREFIX + "requestSentRetryEnabled";
  public static final Boolean REQUEST_SENT_RETRY_ENABLED_DEFAULT = Boolean.FALSE;

  public static final ConfigDef CONFIG = baseConfigDef();

  protected static ConfigDef baseConfigDef() {
    final ConfigDef configDef = new ConfigDef();
    addConfig(configDef);
    return configDef;
  }

  public static void addConfig(ConfigDef config) {

    config
        .define(CONNECT_TIMEOUT_CONFIG, Type.INT, CONNECT_TIMEOUT_DEFAULT, null, Importance.MEDIUM, "Http Client Connect timeout")
        .define(SOCKET_TIMEOUT_CONFIG, Type.INT, SOCKET_TIMEOUT_DEFAULT, null, Importance.MEDIUM, "Http Client Socket timeout")
        .define(CONNECTION_REQUEST_TIMEOUT_CONFIG, Type.INT, CONNECTION_REQUEST_TIMEOUT_DEFAULT, null, Importance.MEDIUM, "Http Client Connection request timeout")

        .define(RETRY_COUNT_CONFIG, Type.INT, RETRY_COUNT_DEFAULT, null, Importance.MEDIUM, "how many times to retry; 0 means no retries. default: " + RETRY_COUNT_DEFAULT)
        .define(REQUEST_SENT_RETRY_ENABLED_CONFIG, Type.BOOLEAN, REQUEST_SENT_RETRY_ENABLED_DEFAULT, null, Importance.MEDIUM,
            "true if it's OK to retry non-idempotent requests that have been sent. default: " + REQUEST_SENT_RETRY_ENABLED_DEFAULT)
        .withClientSslSupport();

  }

  private HttpClientConfig(Map<String, ?> props) {
    super(CONFIG, props);
  }

  public static HttpClient buildClient(Map<String, ?> props) {

    HttpClientConfig conf = new HttpClientConfig(props);

    HttpClientBuilder hcb = HttpClientBuilder.create();
    //Request defaults 
    RequestConfig.Builder rcb = RequestConfig.custom()
        .setConnectTimeout(conf.getInt(CONNECT_TIMEOUT_CONFIG))
        .setSocketTimeout(conf.getInt(SOCKET_TIMEOUT_CONFIG))
        .setConnectionRequestTimeout(conf.getInt(CONNECTION_REQUEST_TIMEOUT_CONFIG));
    hcb.setDefaultRequestConfig(rcb.build());

    DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(conf.getInt(RETRY_COUNT_CONFIG), conf.getBoolean(REQUEST_SENT_RETRY_ENABLED_CONFIG));
    hcb.setRetryHandler(retryHandler);

    try {
      hcb.setSSLContext(SslUtils.createSSLContext(conf));
    } catch (Exception e) {
      throw new ConfigException("Failed to configure SSLContext", e);
    }

    return hcb.build();
  }

}
