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

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

import java.util.Map;

public class QueueBatchConfig extends AbstractConfig {

  public static final String QUEUE_CLASS_CONFIG = "connector.queue.class";

  public static final String QUEUE_TIMEOUT_CONFIG = "connector.queue.batchTimeout";
  public static final Long QUEUE_TIMEOUT_DEFAULT = 5000L;

  public static final String QUEUE_BATCH_CONFIG = "connector.queue.batchSize";
  public static final Integer QUEUE_BATCH_DEFAULT = 1000;

  public static final String QUEUE_CAPACITY_CONFIG = "connector.queue.capacity";

  public static final ConfigDef CONFIG = baseConfigDef();

  protected static ConfigDef baseConfigDef() {
    final ConfigDef configDef = new ConfigDef();
    addConfig(configDef);
    return configDef;
  }

  public static void addConfig(ConfigDef config) {
    final String group = "Queue Configs";
    int order = 0;
    config
        .define(QUEUE_CLASS_CONFIG, Type.CLASS, null, Importance.MEDIUM, "Blocking queue impl", group, ++order, Width.LONG, "BlockingQueue impl")
        .define(QUEUE_TIMEOUT_CONFIG, Type.LONG, QUEUE_TIMEOUT_DEFAULT, Importance.MEDIUM, "drain max timeout", group, ++order, Width.LONG, "drain max timeout")
        .define(QUEUE_BATCH_CONFIG, Type.INT, QUEUE_BATCH_DEFAULT, Importance.MEDIUM, "batch size", group, ++order, Width.LONG, "batch size")
        .define(QUEUE_CAPACITY_CONFIG, Type.INT, Integer.MAX_VALUE, Importance.MEDIUM, "max queue size", group, ++order, Width.LONG, "max queue size");
  }

  public static ConfigDef extendConfig(ConfigDef config) {
    final String group = "Queue Configs";
    int order = 0;
    config
        .define(QUEUE_CLASS_CONFIG, Type.CLASS, null, Importance.MEDIUM, "Blocking queue impl", group, ++order, Width.LONG, "BlockingQueue impl")
        .define(QUEUE_TIMEOUT_CONFIG, Type.LONG, QUEUE_TIMEOUT_DEFAULT, Importance.MEDIUM, "drain max timeout", group, ++order, Width.LONG, "drain max timeout")
        .define(QUEUE_BATCH_CONFIG, Type.INT, QUEUE_BATCH_DEFAULT, Importance.MEDIUM, "batch size", group, ++order, Width.LONG, "batch size")
        .define(QUEUE_CAPACITY_CONFIG, Type.INT, Integer.MAX_VALUE, Importance.MEDIUM, "max queue size", group, ++order, Width.LONG, "max queue size");
    return config;
  }

  public QueueBatchConfig(Map<String, ?> props) {
    super(CONFIG, props);
  }

}
