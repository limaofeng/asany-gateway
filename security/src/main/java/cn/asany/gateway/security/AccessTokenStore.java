package cn.asany.gateway.security;

import net.asany.jfantasy.framework.security.auth.core.AbstractTokenStore;
import net.asany.jfantasy.framework.security.auth.oauth2.core.OAuth2AccessToken;
import org.springframework.data.redis.core.StringRedisTemplate;

public class AccessTokenStore extends AbstractTokenStore<OAuth2AccessToken> {
  public AccessTokenStore(StringRedisTemplate redisTemplate) {
    super(redisTemplate, "token");
  }
}
