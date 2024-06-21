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
package cn.asany.gateway.boot;

import com.fasterxml.jackson.annotation.JsonInclude;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.tools.SchemaParser;
import java.io.IOException;
import net.asany.jfantasy.graphql.gateway.*;
import net.asany.jfantasy.graphql.gateway.service.DefaultGraphQLClientFactory;
import net.asany.jfantasy.graphql.gateway.type.ScalarTypeProviderFactory;
import net.asany.jfantasy.graphql.gateway.type.ScalarTypeResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestTemplate;

/**
 * 应用程序入口
 *
 * @author limaofeng
 * @version V1.0
 */
@Configuration
@EnableCaching
@ComponentScan(basePackages = {"cn.asany.gateway.boot.graphql"})
@EnableAutoConfiguration(
    exclude = {
      MongoAutoConfiguration.class,
      RedisRepositoriesAutoConfiguration.class,
      ElasticsearchRepositoriesAutoConfiguration.class
    })
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(Application.class);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(Application.class);
  }

  @Bean(initMethod = "init", destroyMethod = "destroy")
  public GraphQLGateway graphqlGateway(
      SchemaParser schemaParser,
      GraphQLClientFactory clientFactory,
      ScalarTypeProviderFactory scalarFactory)
      throws IOException {
    return GraphQLGateway.builder()
        .schema(schemaParser.makeExecutableSchema())
        .clientFactory(clientFactory)
        .scalarResolver(new ScalarTypeResolver(scalarFactory))
        .config("classpath:graphql-gateway.yaml")
        .build();
  }

  @Bean
  public GraphQLClientFactory graphQLClientFactory(
      ResourceLoader resourceLoader, RestTemplate restTemplate, GraphQLObjectMapper objectMapper) {
    return new DefaultGraphQLClientFactory(
        resourceLoader,
        restTemplate,
        objectMapper
            .getJacksonMapper()
            .copy()
            .setSerializationInclusion(JsonInclude.Include.ALWAYS));
  }

  @Bean
  public GraphQLReloadSchemaProvider graphqlSchemaProvider(GraphQLGateway graphQLGateway) {
    return new GraphQLGatewayReloadSchemaProvider(graphQLGateway);
  }
}
