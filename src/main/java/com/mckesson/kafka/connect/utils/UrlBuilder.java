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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlBuilder {

  private String url;

  public UrlBuilder(String url) {
    if (url == null)
      throw new NullPointerException("Url can not be null");
    this.url = url;
  }

  public UrlBuilder routeParam(String name, String value) {
    Matcher matcher = Pattern.compile("\\{" + name + "\\}").matcher(url);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    if (count == 0) {
      return this;
      //throw new RuntimeException("Can't find route parameter name \"" + name + "\"");
    }
    this.url = url.replaceAll("\\{" + name + "\\}", urlEncode(value));
    return this;
  }

  public UrlBuilder queryString(String name, Collection<?> value) {
    for (Object cur : value) {
      queryString(name, cur);
    }
    return this;
  }

  public UrlBuilder queryString(String name, Object value) {

    StringBuilder queryString = new StringBuilder();
    if (this.url.contains("?")) {
      queryString.append("&");
    } else {
      queryString.append("?");
    }
    try {
      queryString
          .append(URLEncoder.encode(name))
          .append("=")
          .append(URLEncoder.encode((value == null) ? "" : value.toString(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    this.url += queryString.toString();
    return this;
  }

  public String getUrl() {
    return url;
  }

  public static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
