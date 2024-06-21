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

import cn.asany.gateway.security.domain.converter.DeviceConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenClientDetails {
  /** 访问IP */
  @Column(name = "CLIENT_IP", length = 50)
  private String ip;

  /** 访问位置 */
  @Column(name = "CLIENT_LOCATION", length = 300)
  private String location;

  /** 操作系统和浏览器版本 */
  @Column(name = "CLIENT_DEVICE", length = 500, columnDefinition = "JSON")
  @Convert(converter = DeviceConverter.class)
  private ClientDevice device;

  /** 最后访问IP */
  @Column(name = "CLIENT_LAST_IP", length = 50)
  private String lastIp;

  /** 最后访问位置 */
  @Column(name = "CLIENT_LAST_LOCATION", length = 300)
  private String lastLocation;
}
