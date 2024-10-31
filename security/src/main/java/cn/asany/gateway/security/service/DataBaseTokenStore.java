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
package cn.asany.gateway.security.service;

import cn.asany.gateway.security.domain.AccessToken;
import cn.asany.gateway.security.domain.AccessTokenClientDetails;
import cn.asany.gateway.security.domain.ClientDevice;
import cn.asany.gateway.security.domain.enums.TokenType;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.asany.jfantasy.framework.security.auth.core.AbstractTokenStore;
import net.asany.jfantasy.framework.security.auth.core.AuthenticationDetails;
import net.asany.jfantasy.framework.security.auth.core.ClientRegistrationException;
import net.asany.jfantasy.framework.security.auth.core.ClientSecret;
import net.asany.jfantasy.framework.security.auth.oauth2.JwtTokenPayload;
import net.asany.jfantasy.framework.security.auth.oauth2.core.OAuth2AccessToken;
import net.asany.jfantasy.framework.security.auth.oauth2.jwt.JwtUtils;
import net.asany.jfantasy.framework.security.authentication.Authentication;
import net.asany.jfantasy.framework.util.common.ObjectUtil;
import net.asany.jfantasy.framework.util.common.StringUtil;
import net.asany.jfantasy.framework.util.web.WebUtil;
import net.asany.jfantasy.graphql.client.GraphQLClient;
import net.asany.jfantasy.graphql.client.GraphQLResponse;
import net.asany.jfantasy.graphql.client.GraphQLTemplate;
import net.asany.jfantasy.graphql.client.error.DataFetchGraphQLError;
import net.asany.jfantasy.schedule.service.TaskScheduler;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TokenStore
 *
 * @author limaofeng
 */
@Slf4j
@Service
public class DataBaseTokenStore extends AbstractTokenStore<OAuth2AccessToken> {

  private final AccessTokenService accessTokenService;
  @GraphQLClient
  private GraphQLTemplate client;

  public DataBaseTokenStore(
      StringRedisTemplate redisTemplate,
      AccessTokenService accessTokenService,
      TaskScheduler taskScheduler) {
    super(redisTemplate, "token");
    this.accessTokenService = accessTokenService;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void storeAccessToken(OAuth2AccessToken token, Authentication authentication) {
    Optional<AccessToken> optionalAccessToken =
        this.accessTokenService.getAccessToken(token.getTokenValue());

    HttpServletRequest request = ObjectUtil.getValue("details.request", authentication);

    AccessTokenClientDetails clientDetails = new AccessTokenClientDetails();

    if (request != null) {
      String ip = WebUtil.getClientIP(request);

      IpResult ipResult = null;
      if (ip != null) {
        try {
          ipResult = location(ip);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }

      UserAgent userAgent = WebUtil.parseUserAgent(request);
      clientDetails.setIp(ip);
      clientDetails.setDevice(
          ClientDevice.builder()
              .userAgent(request.getHeader("User-Agent"))
              .browser(userAgent.getBrowser().getName())
              .type(userAgent.getOperatingSystem().getDeviceType())
              .operatingSystem(userAgent.getOperatingSystem().getName())
              .build());
      if (ipResult != null && StringUtil.isNotBlank(ipResult.getProvince())) {
        String location =
          ipResult.getProvince().equals(ipResult.getCity()) || ipResult.getCity() == null
                ? ipResult.getProvince()
                : StringUtil.defaultValue(ipResult.getProvince(), "") + " " + ipResult.getCity();
        clientDetails.setLocation(location.trim());
      }
    }

    // 如果已经存在，更新最后使用时间及位置信息
    if (optionalAccessToken.isEmpty()) {
      JwtTokenPayload payload = JwtUtils.payload(token.getTokenValue());

      clientDetails.setLastIp(clientDetails.getIp());
      clientDetails.setLastLocation(clientDetails.getLocation());

      AuthenticationDetails details = authentication.getDetails();

      ClientSecret clientSecret = details.getClientSecret();

      AccessToken accessToken =
        this.accessTokenService.createAccessToken(
          ObjectUtil.defaultValue(authentication.getName(), payload::getName),
          payload.getUserId(),
          payload.getIss(),
          clientSecret,
          token,
          TokenType.SESSION,
          clientDetails);
      log.debug("accessToken({}) 保存成功!", accessToken.getId());
    } else {
      AccessToken accessToken = optionalAccessToken.get();
      accessToken = this.accessTokenService.updateAccessToken(accessToken, token, clientDetails);
      log.debug("accessToken({}) 更新成功!", accessToken.getId());
    }
    super.storeAccessToken(token, authentication);
  }

  public IpResult location(String ip) {
    Map<String, Object> params = new HashMap<>();
    params.put("ip", ip);
    try {
      GraphQLResponse response = this.client.post("""
        query location($ip: String) {
          result: amapOpenAPI {
            ip(ip: $ip) {
              city
              province
            }
          }
        }
        """, "location", params);
      if (response.hasErrors()) {
        List<DataFetchGraphQLError> errors = response.getList("$.errors", DataFetchGraphQLError.class);
        throw new ClientRegistrationException(errors.get(0).getMessage());
      }
      return response.get("$.data.result.ip", IpResult.class);
    } catch (IOException e) {
      throw new ClientRegistrationException(e.getMessage());
    }
  }

  @Override
  public void removeAccessToken(OAuth2AccessToken token) {
    this.accessTokenService.delete(token.getTokenValue());
    super.removeAccessToken(token);
  }

  @Data
  @ToString
  public static class IpResult {
    private String status;
    private String info;
    private String infocode;
    private String province;
    private String city;
    private String adcode;
    private String rectangle;
  }
}
