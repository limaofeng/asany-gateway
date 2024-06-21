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

import cn.asany.gateway.security.convert.AccessTokenConverter;
import cn.asany.gateway.security.dao.AccessTokenDao;
import cn.asany.gateway.security.domain.AccessToken;
import cn.asany.gateway.security.domain.AccessTokenClientDetails;
import cn.asany.gateway.security.vo.SessionAccessToken;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.asany.jfantasy.framework.dao.jpa.PropertyFilter;
import net.asany.jfantasy.framework.security.auth.TokenType;
import net.asany.jfantasy.framework.security.auth.oauth2.core.OAuth2AccessToken;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 访问令牌服务
 *
 * @author limaofeng
 */
@Slf4j
@Service
public class AccessTokenService {

  private final AccessTokenDao accessTokenDao;
  private final AccessTokenConverter accessTokenConverter;

  public AccessTokenService(
      AccessTokenDao accessTokenDao, AccessTokenConverter accessTokenConverter) {
    this.accessTokenDao = accessTokenDao;
    this.accessTokenConverter = accessTokenConverter;
  }

  /**
   * 创建 AccessToken
   *
   * @param name 名称
   * @param uid 用户ID
   * @param clientId 客户端
   * @param clientSecret 客户端密钥
   * @param token 令牌
   * @param clientDetails 请求详情
   * @return AccessToken
   */
  public AccessToken createAccessToken(
      String name,
      Long uid,
      String clientId,
      String clientSecret,
      OAuth2AccessToken token,
      AccessTokenClientDetails clientDetails) {
    return this.accessTokenDao.save(
        AccessToken.builder()
            .name(name)
            .token(token.getTokenValue())
            .tokenType(token.getTokenType())
            .issuedAt(Date.from(token.getIssuedAt()))
            .expiresAt(token.getExpiresAt() != null ? Date.from(token.getExpiresAt()) : null)
            .scopes(token.getScopes())
            .refreshToken(token.getRefreshTokenValue())
            .client(clientId)
            .clientSecret(clientSecret)
            .lastUsedTime(Date.from(Instant.now()))
            .user(uid)
            .clientDetails(clientDetails)
            .build());
  }

  /**
   * 更新 AccessToken
   *
   * @param accessToken 原 AccessToken
   * @param token 令牌
   * @param clientDetails 请求详情
   * @return AccessToken
   */
  public AccessToken updateAccessToken(
      AccessToken accessToken, OAuth2AccessToken token, AccessTokenClientDetails clientDetails) {
    accessToken.setExpiresAt(token.getExpiresAt() != null ? Date.from(token.getExpiresAt()) : null);

    AccessTokenClientDetails tokenClientDetails = accessToken.getClientDetails();
    if (tokenClientDetails == null) {
      tokenClientDetails = new AccessTokenClientDetails();
      tokenClientDetails.setDevice(clientDetails.getDevice());
    }
    tokenClientDetails.setLastIp(clientDetails.getIp());
    tokenClientDetails.setLastLocation(clientDetails.getLocation());
    accessToken.setClientDetails(tokenClientDetails);
    accessToken.setLastUsedTime(Date.from(Instant.now()));
    this.accessTokenDao.update(accessToken);
    return accessToken;
  }

  public Optional<AccessToken> getAccessToken(Long id) {
    return this.accessTokenDao.findById(id);
  }

  public Optional<AccessToken> getAccessToken(String token) {
    return this.accessTokenDao.findOne(PropertyFilter.newFilter().equal("token", token));
  }

  public SessionAccessToken getSessionById(Long id) {
    Optional<AccessToken> optional = this.accessTokenDao.findById(id);
    return optional.map(accessTokenConverter::toSession).orElse(null);
  }

  public List<SessionAccessToken> getSessions(String clientId, Long uid) {
    PropertyFilter filter =
        PropertyFilter.newFilter()
            .equal("client", clientId)
            .equal("tokenType", TokenType.SESSION_ID)
            .equal("user.id", uid);
    List<AccessToken> accessTokens =
        this.accessTokenDao.findAll(filter, Sort.by("issuedAt").descending());
    return accessTokenConverter.toSessions(accessTokens);
  }

  /**
   * 查询密钥最后使用时间
   *
   * @param client 客户端
   * @param clientSecret 客户端密钥
   * @return Date
   */
  public Date getLastUseTime(String client, String clientSecret) {
    List<AccessToken> accessTokens =
        this.accessTokenDao.findAll(
            PropertyFilter.newFilter().equal("client", client).equal("clientSecret", clientSecret),
            1,
            Sort.by("lastUsedTime").descending());
    if (accessTokens.isEmpty()) {
      return null;
    }
    return accessTokens.get(0).getLastUsedTime();
  }

  @Transactional(rollbackFor = Exception.class)
  public boolean delete(String token) {
    Optional<AccessToken> accessToken = getAccessToken(token);
    accessToken.ifPresent(this.accessTokenDao::delete);
    return accessToken.isPresent();
  }

  @Transactional(rollbackFor = Exception.class)
  public void cleanupExpiredToken(String tokenValue) {
    boolean isDeleted = delete(tokenValue);
    log.info("删除过期的访问令牌：{} {}", tokenValue, isDeleted ? "成功" : "失败");
  }
}
