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

import jakarta.persistence.*;
import java.util.List;
import java.util.Objects;
import lombok.*;
import net.asany.jfantasy.framework.dao.BaseBusEntity;
import net.asany.jfantasy.framework.security.auth.core.ClientSecret;
import net.asany.jfantasy.framework.security.auth.core.ClientSecretType;
import org.hibernate.Hibernate;

/**
 * 客户端凭证
 *
 * @author limaofeng
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "AUTH_CLIENT_SECRET",
    uniqueConstraints = {@UniqueConstraint(name = "UK_CLIENT_ID", columnNames = "CLIENT_ID")})
public class ApplicationClientSecret extends BaseBusEntity implements ClientSecret {

  @Id
  @Column(name = "ID")
  private String id;

  /** 密钥类型 */
  @Column(name = "TYPE", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private ClientSecretType type;

  /** 密钥 */
  @Column(name = "SECRET", length = 40, updatable = false)
  private String secret;

  /** 客户端 */
  @Column(name = "CLIENT_ID", length = 20, updatable = false, nullable = false)
  private String client;

  /** 访问令牌 */
  @SuppressWarnings("JpaQlInspection")
  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @OrderBy("createdAt desc")
  @JoinColumn(name = "CLIENT_SECRET", referencedColumnName = "SECRET", updatable = false)
  @ToString.Exclude
  private List<AccessToken> accessTokens;

  @Builder.Default
  @Column(name = "TOKEN_EXPIRES")
  private Integer tokenExpires = 30;

  @Override
  @Transient
  public String getSecretValue() {
    return this.secret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    ApplicationClientSecret that = (ApplicationClientSecret) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
