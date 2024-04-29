package cn.asany.gateway.boot;

import com.fasterxml.jackson.annotation.JsonInclude;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.kickstart.tools.SchemaParser;
import java.io.IOException;
import net.asany.jfantasy.graphql.gateway.GraphQLGateway;
import net.asany.jfantasy.graphql.gateway.GraphQLGatewayReloadSchemaProvider;
import net.asany.jfantasy.graphql.gateway.GraphQLReloadSchemaProvider;
import net.asany.jfantasy.graphql.gateway.GraphQLTemplateFactory;
import net.asany.jfantasy.graphql.gateway.service.DefaultGraphQLTemplateFactory;
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
      GraphQLTemplateFactory templateFactory,
      ScalarTypeProviderFactory scalarFactory)
      throws IOException {
    return GraphQLGateway.builder()
        .schema(schemaParser.makeExecutableSchema())
        .clientFactory(templateFactory)
        .scalarResolver(new ScalarTypeResolver(scalarFactory))
        .config("graphql-gateway.yaml")
        .build();
  }

  @Bean
  public GraphQLTemplateFactory graphQLTemplateFactory(
      ResourceLoader resourceLoader, RestTemplate restTemplate, GraphQLObjectMapper objectMapper) {
    return new DefaultGraphQLTemplateFactory(
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
