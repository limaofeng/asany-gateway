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
package cn.asany.gateway.security.domain;

import cn.asany.gateway.security.domain.enums.ApplicationType;
import cn.asany.gateway.security.service.ApplicationService;
import jakarta.persistence.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.*;
import net.asany.jfantasy.framework.dao.BaseBusEntity;
import net.asany.jfantasy.framework.dao.hibernate.annotations.TableGenerator;
import net.asany.jfantasy.framework.security.auth.core.ClientDetails;
import net.asany.jfantasy.framework.security.auth.core.ClientSecret;
import net.asany.jfantasy.framework.security.auth.core.ClientSecretType;
import net.asany.jfantasy.framework.security.core.GrantedAuthority;
import net.asany.jfantasy.framework.spring.SpringBeanUtils;
import org.hibernate.Hibernate;

/**
 * 应用信息
 *
 * @author limaofeng
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "NUWA_APPLICATION",
    uniqueConstraints = {
      @UniqueConstraint(name = "UK_APPLICATION_NAME", columnNames = "NAME"),
      @UniqueConstraint(name = "UK_APPLICATION_CLIENT_ID", columnNames = "CLIENT_ID")
    })
public class Application extends BaseBusEntity implements ClientDetails {
  /** ID */
  @Id
  @Column(name = "ID")
  @TableGenerator
  private Long id;

  /** 名称 (全英文) */
  @Column(name = "NAME", length = 50)
  private String name;

  /** 应用类型 */
  @Enumerated(EnumType.STRING)
  @Column(name = "TYPE", length = 20, nullable = false)
  private ApplicationType type;

  /** 应用访问地址 */
  @Column(name = "URL")
  private String url;

  /** 是否启用 */
  @Builder.Default
  @Column(name = "ENABLED")
  private Boolean enabled = true;

  /** 标题 */
  @Column(name = "TITLE")
  private String title;

  /** 简介 */
  @Column(name = "DESCRIPTION")
  private String description;

  /** LOGO */
  @Column(name = "LOGO")
  private String logo;

  /** 授权回调 URL */
  @Column(name = "CALLBACK_URL", length = 100)
  private String callbackUrl;

  /** 客服端 ID */
  @Column(name = "CLIENT_ID", length = 20, updatable = false, nullable = false)
  private String clientId;

  /** 客服端密钥 */
  @OrderBy(" createdAt desc ")
  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @JoinColumn(name = "CLIENT_ID", referencedColumnName = "CLIENT_ID", updatable = false)
  @ToString.Exclude
  private Set<ApplicationClientSecret> clientSecretsAlias;

  @Builder.Default
  @Column(name = "TOKEN_EXPIRES")
  private Integer tokenExpires = 30;

  @Override
  @Transient
  public Map<String, Object> getAdditionalInformation() {
    return new Hashtable<>();
  }

  @Override
  @Transient
  public <T> T getAdditionalInformation(String key, Class<T> clazz) {
    return null;
  }

  @Override
  public Collection<GrantedAuthority> getAuthorities() {
    return new ArrayList<>();
  }

  @Override
  public Set<String> getAuthorizedGrantTypes() {
    return new HashSet<>();
  }

  @Override
  @Transient
  public Set<ClientSecret> getClientSecrets(ClientSecretType type) {
    return getClientSecretsAlias().stream()
        .filter(item -> item.getType() == type)
        .collect(Collectors.toSet());
  }

  @Override
  @Transient
  public Optional<ClientSecret> getClientSecret(ClientSecretType type) {
    return this.getClientSecretsAlias().stream()
        //        .filter(s -> type.equals(s.getType()))
        .map(s -> (ClientSecret) s)
        .findAny();
  }

  @Override
  @Transient
  public Optional<ClientSecret> getClientSecret(String id) {
    return this.getClientSecretsAlias().stream()
        .filter(s -> id.equals(s.getId()))
        .map(s -> (ClientSecret) s)
        .findAny();
  }

  @Override
  public String getRedirectUri() {
    return null;
  }

  @Override
  public Set<String> getScope() {
    return new HashSet<>();
  }

  @Override
  @Transient
  public String getTenantId() {
    if (this.getType() == ApplicationType.Native) {
      ApplicationService applicationService =
          SpringBeanUtils.getBeanByType(ApplicationService.class);
      return applicationService.getTenantIdByNativeApplicationId(this.id);
    }
    return null;
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Application that = (Application) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }
}
