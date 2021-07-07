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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class DateTimeUtils {

  private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz").withZone(ZoneOffset.UTC);

  private static final DateTimeFormatter[] FORMATTERS = {
      DEFAULT_FORMATTER,
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSz").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssz").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.Sz").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSz").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSz").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSz").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSz").withZone(ZoneOffset.UTC) };

  public static LocalDateTime parseDateTime(String dtString) {
    return parseDateTime(dtString, null);
  }

  public static ZonedDateTime parseZonedDateTime(String dtString) {
    return parseZonedDateTime(dtString, null);
  }

  public static LocalDateTime parseDateTime(String dtString, DateTimeFormatter formatter) {
    if (dtString == null) {
      throw new NullPointerException("null DateTime string");
    }
    if (formatter == null) {
      DateTimeParseException ex = null;
      for (DateTimeFormatter dtf : FORMATTERS) {
        try {
          return LocalDateTime.parse(dtString, dtf);
        } catch (DateTimeParseException e) {
          ex = e;
        }
      }
      throw ex; //throw the last exception
    }
    return LocalDateTime.parse(dtString, formatter);
  }

  public static ZonedDateTime parseZonedDateTime(String dtString, DateTimeFormatter formatter) {
    if (dtString == null) {
      throw new NullPointerException("null DateTime string");
    }
    if (formatter == null) {
      DateTimeParseException ex = null;
      for (DateTimeFormatter dtf : FORMATTERS) {
        try {
          return ZonedDateTime.parse(dtString, dtf);
        } catch (DateTimeParseException e) {
          ex = e;
        }
      }
      throw ex; //throw the last exception
    }
    return ZonedDateTime.parse(dtString, formatter);
  }

  public static Date parseDate(String dtString) {
    return Date.from(parseDateTime(dtString, null).atZone(ZoneId.systemDefault()).toInstant());
  }

  public static Date parseDate(String dtString, DateTimeFormatter formatter) {
    return Date.from(parseDateTime(dtString, formatter).atZone(ZoneId.systemDefault()).toInstant());
  }

  public static String printMillis(long millis) {
    return printMillis(millis, ZoneId.systemDefault(), null);
  }

  public static String printMillis(long millis, DateTimeFormatter formatter) {
    return printMillis(millis, ZoneId.systemDefault(), formatter);
  }


  public static String printUTCMillis(long millis) {
    return printMillis(millis, ZoneOffset.UTC, null);
  }
  
  public static String printUTCMillis(long millis, DateTimeFormatter formatter) {
    return printMillis(millis, ZoneOffset.UTC, formatter);
  }

  public static String printMillis(long millis, ZoneId zone, DateTimeFormatter formatter) {

    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone);
    if (formatter == null) {
      return date.format(DEFAULT_FORMATTER);
    }
    return date.format(formatter);
  }

  public static String printDateTime(LocalDateTime dt) {
    return printDateTime(dt, null);
  }

  public static String printDateTime(ZonedDateTime dt) {
    return printDateTime(dt, null);
  }

  public static String printDate(Date dt) {
    return printDateTime(LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault()), null);
  }

  public static String printDate(Date dt, DateTimeFormatter formatter) {
    return printDateTime(LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault()), formatter);
  }

  public static String printDateTime(LocalDateTime dt, DateTimeFormatter formatter) {
    if (dt == null) {
      throw new NullPointerException("LocalDateTime is null");
    }

    if (formatter == null) {
      return dt.format(DEFAULT_FORMATTER);
    }
    return dt.format(formatter);
  }

  public static String printDateTime(ZonedDateTime dt, DateTimeFormatter formatter) {
    if (dt == null) {
      throw new NullPointerException("ZonedDateTime is null");
    }

    if (formatter == null) {
      return dt.format(DEFAULT_FORMATTER);
    }
    return dt.format(formatter);
  }

  /**
  
  
  /**
   * Safe LocalDateTime  max selection, if any value is null  start of epoch is used.
   * 
   * @param dt1
   * @param dt2
   * @return
   */
  public static LocalDateTime max(LocalDateTime dt1, LocalDateTime dt2) {
    return max(dt1, dt2, LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
  }

  public static ZonedDateTime max(ZonedDateTime dt1, ZonedDateTime dt2) {
    return max(dt1, dt2, ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
  }

  public static LocalDateTime max(LocalDateTime dt1, LocalDateTime dt2, LocalDateTime ifNull) {
    LocalDateTime d1 = dt1 != null ? dt1 : ifNull;
    LocalDateTime d2 = dt2 != null ? dt2 : ifNull;

    if (d1.compareTo(d2) >= 0) {
      return d1;
    } else {
      return d2;
    }
  }

  public static ZonedDateTime max(ZonedDateTime dt1, ZonedDateTime dt2, ZonedDateTime ifNull) {
    ZonedDateTime d1 = dt1 != null ? dt1 : ifNull;
    ZonedDateTime d2 = dt2 != null ? dt2 : ifNull;

    if (d1.compareTo(d2) >= 0) {
      return d1;
    } else {
      return d2;
    }
  }
  
  
  
  /**
   * Safe LocalDateTime  max selection, if any value is null  start of epoch is used.
   * 
   * @param dt1
   * @param dt2
   * @return
   */
  public static LocalDateTime min(LocalDateTime dt1, LocalDateTime dt2) {
    return min(dt1, dt2, LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
  }

  public static ZonedDateTime min(ZonedDateTime dt1, ZonedDateTime dt2) {
    return min(dt1, dt2, ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault()));
  }

  public static LocalDateTime min(LocalDateTime dt1, LocalDateTime dt2, LocalDateTime ifNull) {
    LocalDateTime d1 = dt1 != null ? dt1 : ifNull;
    LocalDateTime d2 = dt2 != null ? dt2 : ifNull;

    if (d1.compareTo(d2) >= 0) {
      return d2;
    } else {
      return d1;
    }
  }

  public static ZonedDateTime min(ZonedDateTime dt1, ZonedDateTime dt2, ZonedDateTime ifNull) {
    ZonedDateTime d1 = dt1 != null ? dt1 : ifNull;
    ZonedDateTime d2 = dt2 != null ? dt2 : ifNull;

    if (d1.compareTo(d2) >= 0) {
      return d2;
    } else {
      return d1;
    }
  }

}
