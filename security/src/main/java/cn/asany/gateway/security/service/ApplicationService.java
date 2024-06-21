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

import cn.asany.gateway.security.dao.ApplicationDao;
import cn.asany.gateway.security.domain.Application;
import java.util.Optional;
import net.asany.jfantasy.framework.dao.hibernate.util.HibernateUtils;
import net.asany.jfantasy.framework.dao.jpa.PropertyFilter;
import net.asany.jfantasy.framework.security.auth.core.ClientDetails;
import net.asany.jfantasy.framework.security.auth.core.ClientDetailsService;
import net.asany.jfantasy.framework.security.auth.core.ClientRegistrationException;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService implements ClientDetailsService {

  public static final String CACHE_KEY = "NUWA";

  private final ApplicationDao applicationDao;

  public ApplicationService(ApplicationDao applicationDao) {
    this.applicationDao = applicationDao;
  }

  @Override
  @Cacheable(key = "targetClass + '.' +  methodName + '#' + #p0", value = CACHE_KEY)
  public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
    Optional<Application> optional =
        this.applicationDao.findOne(
            PropertyFilter.newFilter().equal("clientId", clientId).equal("enabled", true));
    if (optional.isEmpty()) {
      throw new ClientRegistrationException("[client_id=" + clientId + "]不存在");
    }
    return HibernateUtils.cloneEntity(
        optional
            .map(
                item -> {
                  Hibernate.initialize(item.getClientSecretsAlias());
                  return item;
                })
            .map(
                item -> {
                  item.setTokenExpires(1);
                  return item;
                })
            .get());
  }
}
