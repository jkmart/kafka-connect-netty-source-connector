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

import com.mckesson.kafka.connect.nettysource.NettyPipelineFactory;
import com.mckesson.kafka.connect.nettysource.NettySourceConnectorConfig;
import com.mckesson.kafka.connect.nettysource.SinglePacketHandler;
import com.mckesson.kafka.connect.nettysource.SourceRecordHandler;
import org.jboss.netty.channel.ChannelHandler;

import java.util.LinkedHashMap;

public class BytesUdpPipelineFactory extends NettyPipelineFactory {

  public LinkedHashMap<String, ChannelHandler> defaultHandlers(NettySourceConnectorConfig conf) {
    LinkedHashMap<String, ChannelHandler> defaultHandlers = new LinkedHashMap<>();
    defaultHandlers.put("framer", new SinglePacketHandler());
    SourceRecordHandler handler = new BytesRecordHandler();
    handler.setTopic(topic);
    handler.setRecordQueue(messageQueue);
    defaultHandlers.put("recordHandler", handler);
    return defaultHandlers;
  }

}
