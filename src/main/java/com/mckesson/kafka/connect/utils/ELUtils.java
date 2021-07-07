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
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.header.Header;

import java.text.MessageFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ELUtils {

  private static final Pattern HAS_EL_PATTERN = Pattern.compile("\\$\\{(KEY|VALUE|TOPIC|PARTITION|TIMESTAMP\\,\\w+|HEADER\\.[\\w\\d\\.]*)\\}", Pattern.CASE_INSENSITIVE);

  private static final Pattern VALUE_PATTERN = Pattern.compile("\\$\\{VALUE\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern KEY_PATTERN = Pattern.compile("\\$\\{KEY\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern TOPIC_PATTERN = Pattern.compile("\\$\\{TOPIC\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern PARTITION_PATTERN = Pattern.compile("\\$\\{PARTITION\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\$\\{TIMESTAMP\\s*,\\w+\\}", Pattern.CASE_INSENSITIVE);
  private static final Pattern TS_REPL_PATTERN = Pattern.compile("\\$\\{TIMESTAMP", Pattern.CASE_INSENSITIVE);
  private static final Pattern HEADERS_PATTERN = Pattern.compile("\\$\\{HEADER\\.([\\d\\w\\.]*)\\}", Pattern.CASE_INSENSITIVE);

  public static boolean containsEL(String expression) {
    if (StringUtils.isBlank(expression)) {
      return false;
    }
    return HAS_EL_PATTERN.matcher(expression).find();
  }

  public static <R extends ConnectRecord<R>> String getExprValue(String expression, R rec) {

    if (!containsEL(expression)) {
      return expression;
    }

    String newValue = expression;
    //replace all but headers
    newValue = VALUE_PATTERN.matcher(newValue).replaceAll(rec.value() == null ? "" : Matcher.quoteReplacement(rec.value().toString()));
    newValue = KEY_PATTERN.matcher(newValue).replaceAll(rec.key() == null ? "" : Matcher.quoteReplacement(rec.key().toString()));
    newValue = TOPIC_PATTERN.matcher(newValue).replaceAll(rec.topic() == null ? "" : Matcher.quoteReplacement(rec.topic()));
    newValue = PARTITION_PATTERN.matcher(newValue).replaceAll(rec.kafkaPartition() == null ? "" : Matcher.quoteReplacement(rec.kafkaPartition().toString()));

    //replace headers
    Matcher m = HEADERS_PATTERN.matcher(newValue);
    while (m.find()) {
      String hdrName = m.group(1);
      Header hdrValue = rec.headers().lastWithName(hdrName);
      newValue = newValue.replaceAll("(?i)\\$\\{HEADER\\." + hdrName + "\\}", hdrValue == null ? "" : Matcher.quoteReplacement("" + hdrValue.value()));
    }

    if (TIMESTAMP_PATTERN.matcher(newValue).find()) {
      Date dt = (rec.timestamp() == null || rec.timestamp() == 0) ? new Date() : new Date(rec.timestamp());
      String msgTempate = TS_REPL_PATTERN.matcher(newValue).replaceAll("{0,date");
      newValue = MessageFormat.format(msgTempate, dt);
    }

    return newValue;

  }

}
