package cn.asany.gateway.autoconfigure;

import cn.asany.gateway.security.AccessTokenStore;
import cn.asany.gateway.security.AppClientDetailsService;
import net.asany.jfantasy.framework.security.auth.core.ClientDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class GatewaySecurityAutoConfiguration {

  @Bean
  public AccessTokenStore AccessTokenStore(StringRedisTemplate redisTemplate) {
    return new AccessTokenStore(redisTemplate);
  }

  @Bean
  public ClientDetailsService clientDetailsService() {
    return new AppClientDetailsService();
  }

}
