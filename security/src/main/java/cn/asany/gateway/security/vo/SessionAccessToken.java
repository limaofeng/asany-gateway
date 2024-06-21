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
package cn.asany.gateway.security.vo;

import cn.asany.gateway.security.domain.ClientDevice;
import java.util.Date;
import lombok.Data;

/**
 * 会话
 *
 * @author limaofeng
 */
@Data
public class SessionAccessToken {
  /** ID */
  private Long id;

  /** 设备 */
  private ClientDevice device;

  /** IP 地址 */
  private String ip;

  /** 最后一次使用的 IP 地址 */
  private String lastIp;

  /** 登录时的位置 */
  private String location;

  /** 最后一次访问的位置 */
  private String lastLocation;

  /** 登录时间 */
  private Date loginTime;

  /** 最后一次访问时间 */
  private Date lastUsedTime;

  /** 令牌 */
  private String token;

  /** 过期时间 */
  private Date expiresAt;

  /** 客户端凭证 ID */
  private String client;
}
