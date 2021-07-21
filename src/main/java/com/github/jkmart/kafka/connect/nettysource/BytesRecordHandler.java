/**
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
package com.github.jkmart.kafka.connect.nettysource;

import com.mckesson.kafka.connect.nettysource.SourceRecordHandler;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public class BytesRecordHandler extends SourceRecordHandler {

  static final Logger LOG = LoggerFactory.getLogger(BytesRecordHandler.class);

  public BytesRecordHandler() {
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

    byte[] msg = null;

    try {
      ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
      msg = buffer.array();
    } catch (Exception exception) {
      exception.printStackTrace();
      LOG.warn("Could not get bytes from buffer", exception);
    }
    if (msg == null) {
      return;
    }
    Map<String, ?> sourcePartition = new HashMap<>();
    Map<String, ?> sourceOffset = new HashMap<>();

    if (recordQueue == null) {
      throw new IllegalStateException("recordQueue is not configured");
    }

    SourceRecord srcRec = new SourceRecord(sourcePartition, sourceOffset, topic, Schema.BYTES_SCHEMA, msg);

    recordQueue.add(srcRec);
    checkQueueCapacity();

  }

}
