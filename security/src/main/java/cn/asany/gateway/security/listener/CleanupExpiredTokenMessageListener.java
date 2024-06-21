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
package cn.asany.gateway.security.listener;

import cn.asany.gateway.security.service.AccessTokenService;
import org.springframework.stereotype.Component;

@Component
public class CleanupExpiredTokenMessageListener {

  private final AccessTokenService accessTokenService;

  public CleanupExpiredTokenMessageListener(AccessTokenService accessTokenService) {
    this.accessTokenService = accessTokenService;
  }

  public void onMessage(String key, String pattern) {
    if (!"__keyevent@0__:expired".equals(pattern)) {
      return;
    }
    if (!key.startsWith("token:")) {
      return;
    }
    accessTokenService.cleanupExpiredToken(key.substring(6));
  }
}
