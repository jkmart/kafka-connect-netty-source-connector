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
import org.apache.kafka.common.config.ConfigDef.Validator;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigUtils {

  public static final String MAP_KEY_CONFIG = "key";
  public static final String MAP_VALUE_CONFIG = "value";

  public static final ConfigDef MAP_ENTRY_CONFIG_DEF = new ConfigDef()
      .define(MAP_KEY_CONFIG, ConfigDef.Type.STRING, null, null, ConfigDef.Importance.HIGH, "Key, default value equeals to $alias")
      .define(MAP_VALUE_CONFIG, ConfigDef.Type.STRING, null, null, ConfigDef.Importance.HIGH, "Value object, required.");

  public static Map<String, String> getMap(AbstractConfig mapConfig, String mapKey) {

    Map<String, String> map = new HashMap<>();

    List<String> entries = mapConfig.getList(mapKey);
    for (String alias : entries) {
      SimpleConfig entryConfig = new SimpleConfig(MAP_ENTRY_CONFIG_DEF, mapConfig.originalsWithPrefix(mapKey + "." + alias + "."));

      String key = entryConfig.getString(MAP_KEY_CONFIG);
      if (StringUtils.isBlank(key)) {
        key = alias;
      }

      String value = entryConfig.getString(MAP_VALUE_CONFIG);
      if (value == null) {
        value = mapConfig.getString(mapKey + "." + alias);
      }
      map.put(key, value);
    }

    return map;
  }

  public static <E extends Enum<E>> E getEnum(AbstractConfig mapConfig, String enumKey, Class<E> enumClass) {
    String enumString = mapConfig.getString(enumKey);
    for (E e : enumClass.getEnumConstants()) {
      if (e.name().equalsIgnoreCase(enumString)) {
        return e;
      }
    }
    throw new ConfigException(enumKey, enumString, "Invalid value for enum: " + enumClass);
  }

  public static <E extends Enum<E>> Validator validEnum(Class<E> enumClass) {
    return new ValidEnum<E>(enumClass);
  }

  public static class ValidEnum<E extends Enum<E>> implements Validator {
    Class<E> enumClass;
    Set<String> validNames;

    public ValidEnum(Class<E> enumClass) {
      this.enumClass = enumClass;
      E[] enums = enumClass.getEnumConstants();
      validNames = new HashSet<>(enums.length);
      for (E e : enumClass.getEnumConstants()) {
        validNames.add(e.name().toUpperCase());
      }

    }

    @Override
    public void ensureValid(final String name, final Object value) {
      if (!validNames.contains(((String) value).toUpperCase())) {
        throw new ConfigException(name, value, "Invalid value for enum: " + enumClass);
      }
    }

    public String toString() {
      return validNames.toString();
    }
  }

}
