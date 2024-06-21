/*
 * Copyright (c) 2024 Asany
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.asany.net/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.asany.gateway.autoconfigure;

import cn.asany.gateway.security.listener.CleanupExpiredTokenMessageListener;
import lombok.extern.slf4j.Slf4j;
import net.asany.jfantasy.framework.dao.jpa.SimpleAnyJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@Slf4j
@EntityScan({"cn.asany.gateway.security.domain"})
@ComponentScan({
  "cn.asany.gateway.security.convert",
  "cn.asany.gateway.security.dao",
  "cn.asany.gateway.security.service",
  "cn.asany.gateway.security.listener",
})
@EnableJpaRepositories(
    basePackages = "cn.asany.gateway.security.dao",
    repositoryBaseClass = SimpleAnyJpaRepository.class)
public class GatewaySecurityAutoConfiguration {

  @Bean
  RedisMessageListenerContainer container(
      RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listenerAdapter, new PatternTopic("__keyevent@0__:expired"));

    return container;
  }

  @Bean
  MessageListenerAdapter listenerAdapter(CleanupExpiredTokenMessageListener listener) {
    return new MessageListenerAdapter(listener, "onMessage");
  }
}
