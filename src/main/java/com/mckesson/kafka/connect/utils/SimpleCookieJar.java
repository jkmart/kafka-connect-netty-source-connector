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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.apache.kafka.common.Configurable;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Simplest possible {@link CookieJar} implementation with expiration
 * 
 * @author Vitalii Rudenskyi
 */
public class SimpleCookieJar implements CookieJar, Configurable {

  public static final String EXPIRE_AFTER_WRITE_CONFIG = "expireAfterWrite";
  public static final Long EXPIRE_AFTER_WRITE_DEFAULT = 5 * 60 * 1000L;

  public static final String EXPIRE_AFTER_ACCESS_CONFIG = "expireAfterAccess";
  public static final Long EXPIRE_AFTER_ACCESS_DEFAULT = 5 * 60 * 1000L;

  public static final String MAXIMUM_SIZE_CONFIG = "maximumSize";
  public static final Long MAXIMUM_SIZE_DEFAULT = 1000L;

  public static final ConfigDef CONFIG_DEF = new ConfigDef()
      .define(EXPIRE_AFTER_WRITE_CONFIG, ConfigDef.Type.LONG, EXPIRE_AFTER_WRITE_DEFAULT, ConfigDef.Importance.MEDIUM, "expireAfterWrite millis")
      .define(EXPIRE_AFTER_ACCESS_CONFIG, ConfigDef.Type.LONG, EXPIRE_AFTER_ACCESS_DEFAULT, ConfigDef.Importance.MEDIUM, "expireAfterAccess millis")
      .define(MAXIMUM_SIZE_CONFIG, ConfigDef.Type.LONG, MAXIMUM_SIZE_DEFAULT, ConfigDef.Importance.MEDIUM, "maximumSize");

  private static final String ALL_KEY = "___ALL_URLS";
  LoadingCache<String, List<Cookie>> cookiestore;

  private Long maximumSize;
  private Long expireAfterWrite;
  private Long expireAfterAccess;

  public SimpleCookieJar() {
    configure(Collections.emptyMap());
  }

  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    try {
      cookiestore.get(ALL_KEY).addAll(cookies);
    } catch (ExecutionException e) {
      //ignore
    }
  }

  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    try {
      return cookiestore.get(ALL_KEY);
    } catch (ExecutionException e) {
      return Collections.emptyList();
    }
  }

  @Override
  public void configure(Map<String, ?> configs) {

    SimpleConfig conf = new SimpleConfig(CONFIG_DEF, configs);

    this.maximumSize = conf.getLong(MAXIMUM_SIZE_CONFIG);
    this.expireAfterAccess = conf.getLong(EXPIRE_AFTER_ACCESS_CONFIG);
    this.expireAfterWrite = conf.getLong(EXPIRE_AFTER_WRITE_CONFIG);

    cookiestore = CacheBuilder.newBuilder()
        .maximumSize(this.maximumSize)
        .expireAfterAccess(this.expireAfterAccess, TimeUnit.MILLISECONDS)
        .expireAfterWrite(this.expireAfterWrite, TimeUnit.MILLISECONDS)
        .build(
            new CacheLoader<String, List<Cookie>>() {
              public List<Cookie> load(String key) {
                return new ArrayList<>();
              }
            });

  }

}
