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

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;

public class SslUtils {

  /**
   * Load configured KeyStore
   *   
   * @param config - configuration
   * @return
   * @throws Exception
   */
  public static KeyStore loadKeyStore(AbstractConfig config) throws Exception {
    return loadKeyStore(config, null);
  }

  /**
   *  Load configured KeyStore
   *  
   * @param config - configuration
   * @param withPrefix - prefix for ssl config
   * @return
   * @throws Exception
   */
  public static KeyStore loadKeyStore(AbstractConfig config, String withPrefix) throws Exception {

    AbstractConfig sslConfig;
    if (StringUtils.isBlank(withPrefix)) {
      sslConfig = config;
    } else {
      sslConfig = new SimpleConfig(new ConfigDef().withClientSslSupport(), config.originalsWithPrefix(withPrefix));
    }
    return loadKeyStore(sslConfig.getString(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG), sslConfig.getString(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG), sslConfig.getPassword(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
  }

  /**
   * Load configured TrustStore
   * 
   * @param config
   * @return
   * @throws Exception
   */
  public static KeyStore loadTrustStore(AbstractConfig config) throws Exception {
    return loadTrustStore(config, null);
  }

  /**
   * Load configured TrustStore
   * 
   * @param config
   * @param withPrefix
   * @return
   * @throws Exception
   */
  public static KeyStore loadTrustStore(AbstractConfig config, String withPrefix) throws Exception {

    AbstractConfig sslConfig;
    if (StringUtils.isBlank(withPrefix)) {
      sslConfig = config;
    } else {
      sslConfig = new SimpleConfig(new ConfigDef().withClientSslSupport(), config.originalsWithPrefix(withPrefix));
    }
    return loadKeyStore(sslConfig.getString(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG), sslConfig.getString(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG), sslConfig.getPassword(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
  }

  /**
   * Load keystore
   * 
   * @param storeType
   * @param storeLoc
   * @param passwd
   * @return 
   *       null if 
   * @throws Exception
   */
  public static KeyStore loadKeyStore(String storeType, String storeLoc, Password passwd) throws Exception {

    if (StringUtils.isBlank(storeLoc)) {
      return null;
    }
    File f = new File(storeLoc);
    if (!f.getCanonicalPath().equals(storeLoc)) {
      throw new ConnectException("Relative Path not allowed:" + storeLoc);
    }

    final InputStream ksStream = Files.newInputStream(Paths.get(f.getCanonicalPath()));

    if (ksStream == null) {
      throw new ConnectException("Could not load keystore:" + f.getCanonicalPath());
    }
    try (InputStream is = ksStream) {
      KeyStore loadedKeystore = KeyStore.getInstance(storeType);
      loadedKeystore.load(is, passwd.value().toCharArray());
      return loadedKeystore;
    }
  }

  public static SSLContext createSSLContext(AbstractConfig config) throws Exception {
    return createSSLContext(config, null, true);
  }

  public static SSLContext createSSLContext(AbstractConfig config, String withPrefix) throws Exception {
    return createSSLContext(config, withPrefix, true);
  }

  public static SSLContext createSSLContext(AbstractConfig co, String withPrefix, boolean client) throws Exception {

    AbstractConfig sslConfig;
    if (StringUtils.isBlank(withPrefix)) {
      sslConfig = co;
    } else {
      sslConfig = new SimpleConfig(new ConfigDef().withClientSslSupport(), co.originalsWithPrefix(withPrefix));
    }

    KeyStore keyStore = loadKeyStore(sslConfig);
    final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    Password sslKeyPass = sslConfig.getPassword(SslConfigs.SSL_KEY_PASSWORD_CONFIG); 
    kmf.init(keyStore, sslKeyPass != null ? sslKeyPass.value().toCharArray() : new char[0]);

    KeyStore trustStore = SslUtils.loadTrustStore(sslConfig);
    final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);

    SSLContext sslContext = SSLContext.getInstance(sslConfig.getString(SslConfigs.SSL_PROTOCOL_CONFIG));
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return sslContext;
  }

}
