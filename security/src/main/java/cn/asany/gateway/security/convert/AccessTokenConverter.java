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
package cn.asany.gateway.security.convert;

import cn.asany.gateway.security.domain.AccessToken;
import cn.asany.gateway.security.vo.SessionAccessToken;
import java.util.List;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    builder = @Builder,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AccessTokenConverter {

  @Mappings({
    @Mapping(source = "id", target = "id"),
    @Mapping(source = "clientDetails.device", target = "device"),
    @Mapping(source = "clientDetails.ip", target = "ip"),
    @Mapping(source = "clientDetails.lastIp", target = "lastIp"),
    @Mapping(source = "clientDetails.location", target = "location"),
    @Mapping(source = "clientDetails.lastLocation", target = "lastLocation"),
    @Mapping(source = "issuedAt", target = "loginTime"),
    @Mapping(source = "lastUsedTime", target = "lastUsedTime"),
  })
  SessionAccessToken toSession(AccessToken accessTokens);

  @IterableMapping(elementTargetType = SessionAccessToken.class)
  List<SessionAccessToken> toSessions(List<AccessToken> accessTokens);
}
