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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public final class SyslogParser {

  public enum SyslogFacility {
    KERN, USER, MAIL, DAEMON, AUTH, SYSLOG, LPR, NEWS, UUCP, CRON, AUTHPRIV, FTP, RESERVED_12, RESERVED_13, RESERVED_14, RESERVED_15, LOCAL0, LOCAL1, LOCAL2, LOCAL3, LOCAL4, LOCAL5, LOCAL6, LOCAL7,
  }

  public enum SyslogSeverity {
    EMERG, ALERT, CRIT, ERR, WARNING, NOTICE, INFO, DEBUG,
  }

  //Syslog fields 
  public static final String SYSLOG_TIMESTAMP = "timestamp";
  public static final String SYSLOG_FACILITY = "facility";
  public static final String SYSLOG_SEVERITY = "severity";
  public static final String SYSLOG_HOSTNAME = "hostname";
  public static final String SYSLOG_LOG_MESSAGE = "logMessage";

  public static final String SYSLOG_APP_NAME = "appName";
  public static final String SYSLOG_PROC_ID = "procId";
  public static final String SYSLOG_MSG_ID = "msgId";
  public static final String SYSLOG_STRUCTURED_DATA = "structuredData";

  public static final String SYSLOG_INVALID = "invalid";

  private static final Logger LOG = LoggerFactory.getLogger(SyslogParser.class);

  private enum MONTHS {
    jan, feb, mar, apr, may, jun, jul, aug, sep, oct, nov, dec
  }

  private static Map<String, MONTHS> monthValueMap = new HashMap<String, MONTHS>() {
    private static final long serialVersionUID = 1L;

    {
      put("jan", MONTHS.jan);
      put("feb", MONTHS.feb);
      put("mar", MONTHS.mar);
      put("apr", MONTHS.apr);
      put("may", MONTHS.may);
      put("jun", MONTHS.jun);
      put("jul", MONTHS.jul);
      put("aug", MONTHS.aug);
      put("sep", MONTHS.sep);
      put("oct", MONTHS.oct);
      put("nov", MONTHS.nov);
      put("dec", MONTHS.dec);
    }
  };

  private SyslogParser() {
    // Utility class
  }

  public static Map<String, Object> parseMessage(String message) {
    try {
      return parse(message.getBytes());
    } catch (Exception e) {
      Map<String, Object> syslogMessage = new HashMap<>();
      syslogMessage.put(SYSLOG_INVALID, Boolean.TRUE);
      syslogMessage.put(SYSLOG_LOG_MESSAGE, message);
      return syslogMessage;
    }
  }

  public static Map<String, Object> parseMessage(byte[] bytes) {

    try {
      return parse(bytes);
    } catch (Exception e) {
      Map<String, Object> syslogMessage = new HashMap<>();
      syslogMessage.put(SYSLOG_INVALID, Boolean.TRUE);
      syslogMessage.put(SYSLOG_LOG_MESSAGE, new String(bytes));
      return syslogMessage;
    }

  }

  public static Map<String, Object> parse(byte[] bytes) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
    byteBuffer.put(bytes);
    byteBuffer.rewind();

    Character charFound = (char) byteBuffer.get();

    SyslogFacility foundFacility = null;
    SyslogSeverity foundSeverity = null;

    while (charFound != '<' && byteBuffer.hasRemaining()) {
      // Ignore noise in beginning of message.
      charFound = (char) byteBuffer.get();
    }
    char priChar = 0;
    if (charFound == '<') {
      int facility = 0;

      while (Character.isDigit(priChar = (char) (byteBuffer.get() & 0xff))) {
        facility *= 10;
        facility += Character.digit(priChar, 10);
      }
      foundFacility = SyslogFacility.values()[facility >> 3];
      foundSeverity = SyslogSeverity.values()[facility & 0x07];
    }

    Map<String, Object> syslogMessage = new HashMap<>();
    if (priChar != '>') {
      // Invalid character - this is not a well defined syslog message.
      LOG.trace("Invalid syslog message, missing a > in the Facility/Priority part");
      syslogMessage.put(SYSLOG_LOG_MESSAGE, new String(bytes));
      syslogMessage.put(SYSLOG_INVALID, Boolean.TRUE);
      return syslogMessage;
    }

    boolean isRfc5424 = false;
    // Read next character
    charFound = (char) byteBuffer.get();
    // If next character is a 1, we have probably found an rfc 5424 message
    // message
    if (charFound == '1') {
      isRfc5424 = true;
    } else {
      // go back one to parse the rfc3164 date
      byteBuffer.position(byteBuffer.position() - 1);
    }

    syslogMessage.put(SYSLOG_FACILITY, foundFacility);
    syslogMessage.put(SYSLOG_SEVERITY, foundSeverity);

    if (!isRfc5424) {
      // Parse rfc 3164 date
      syslogMessage.put(SYSLOG_TIMESTAMP, parseRfc3164Date(byteBuffer).getTimeInMillis());
    } else {

      charFound = (char) byteBuffer.get();
      if (charFound != ' ') {
        LOG.trace("Invalid syslog message, missing a mandatory space after version");
        syslogMessage.put(SYSLOG_LOG_MESSAGE, new String(bytes));
        syslogMessage.put(SYSLOG_INVALID, Boolean.TRUE);
        return syslogMessage;

      }

      // This should be the timestamp
      StringBuilder date = new StringBuilder();
      while ((charFound = (char) (byteBuffer.get() & 0xff)) != ' ') {
        date.append(charFound);
      }

      try {
        Calendar ts = DatatypeConverter.parseDateTime(date.toString());
        syslogMessage.put(SYSLOG_TIMESTAMP, ts.getTimeInMillis());
      } catch (Exception e) {
        LOG.trace("Invalid syslog message, failed to parse date: {}", date.toString(), e);
      }

    }

    // The host is the char sequence until the next ' '

    StringBuilder host = new StringBuilder();
    while ((charFound = (char) (byteBuffer.get() & 0xff)) != ' ') {
      host.append(charFound);
    }

    syslogMessage.put(SYSLOG_HOSTNAME, host.toString());

    if (isRfc5424) {

      StringBuilder appName = new StringBuilder();
      while ((charFound = (char) (byteBuffer.get() & 0xff)) != ' ') {
        appName.append(charFound);
      }
      syslogMessage.put(SYSLOG_APP_NAME, appName.toString());

      StringBuilder procId = new StringBuilder();
      while ((charFound = (char) (byteBuffer.get() & 0xff)) != ' ') {
        procId.append(charFound);
      }
      syslogMessage.put(SYSLOG_PROC_ID, procId.toString());

      StringBuilder msgId = new StringBuilder();
      while ((charFound = (char) (byteBuffer.get() & 0xff)) != ' ') {
        msgId.append(charFound);
      }
      syslogMessage.put(SYSLOG_MSG_ID, msgId.toString());

      StringBuilder structuredData = new StringBuilder();
      boolean inblock = false;
      while (((charFound = (char) (byteBuffer.get() & 0xff)) != ' ') || inblock) {
        if (charFound == '[') {
          inblock = true;
        }
        if (charFound == ']') {
          inblock = false;
        }
        structuredData.append(charFound);
      }
      syslogMessage.put(SYSLOG_STRUCTURED_DATA, structuredData.toString());
    }

    StringBuilder msg = new StringBuilder();
    while (byteBuffer.hasRemaining()) {
      charFound = (char) (byteBuffer.get() & 0xff);
      msg.append(charFound);
    }

    syslogMessage.put(SYSLOG_LOG_MESSAGE, msg.toString());
    return syslogMessage;
  }

  private static Calendar parseRfc3164Date(ByteBuffer byteBuffer) {
    char charFound;

    // Done parsing severity and facility
    // <169>Oct 22 10:52:01 TZ-6 scapegoat.dmz.example.org 10.1.2.3
    // sched[0]: That's All Folks!
    // Need to parse the date.

    /**
     * The TIMESTAMP field is the local time and is in the format of
     * "Mmm dd hh:mm:ss" (without the quote marks) where: Mmm is the English
     * language abbreviation for the month of the year with the first
     * character in uppercase and the other two characters in lowercase. The
     * following are the only acceptable values: Jan, Feb, Mar, Apr, May,
     * Jun, Jul, Aug, Sep, Oct, Nov, Dec dd is the day of the month. If the
     * day of the month is less than 10, then it MUST be represented as a
     * space and then the number. For example, the 7th day of August would
     * be represented as "Aug  7", with two spaces between the "g" and the
     * "7". hh:mm:ss is the local time. The hour (hh) is represented in a
     * 24-hour format. Valid entries are between 00 and 23, inclusive. The
     * minute (mm) and second (ss) entries are between 00 and 59 inclusive.
     */

    char[] month = new char[3];
    for (int i = 0; i < 3; i++) {
      month[i] = (char) (byteBuffer.get() & 0xff);
    }
    charFound = (char) byteBuffer.get();
    if (charFound != ' ') {
      // Invalid Message - missing mandatory space.
      LOG.trace("Invalid syslog message, missing a mandatory space after month");
      return null;
    }
    charFound = (char) (byteBuffer.get() & 0xff);

    int day = 0;
    if (charFound == ' ') {
      // Extra space for the day - this is okay.
      // Just ignored per the spec.
    } else {
      day *= 10;
      day += Character.digit(charFound, 10);
    }

    while (Character.isDigit(charFound = (char) (byteBuffer.get() & 0xff))) {
      day *= 10;
      day += Character.digit(charFound, 10);
    }

    int hour = 0;
    while (Character.isDigit(charFound = (char) (byteBuffer.get() & 0xff))) {
      hour *= 10;
      hour += Character.digit(charFound, 10);
    }

    int minute = 0;
    while (Character.isDigit(charFound = (char) (byteBuffer.get() & 0xff))) {
      minute *= 10;
      minute += Character.digit(charFound, 10);
    }

    int second = 0;
    while (Character.isDigit(charFound = (char) (byteBuffer.get() & 0xff))) {
      second *= 10;
      second += Character.digit(charFound, 10);
    }

    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.MONTH, monthValueMap.get(String.valueOf(month).toLowerCase()).ordinal());
    calendar.set(Calendar.DAY_OF_MONTH, day);
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, minute);
    calendar.set(Calendar.SECOND, second);

    return calendar;
  }
}