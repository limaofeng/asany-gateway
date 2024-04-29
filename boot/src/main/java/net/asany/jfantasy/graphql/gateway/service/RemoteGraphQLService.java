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
package net.asany.jfantasy.graphql.gateway.service;

import static graphql.schema.idl.SchemaPrinter.Options.defaultOptions;

import graphql.introspection.IntrospectionQueryBuilder;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.*;
import graphql.schema.DataFetcherFactory;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.*;
import java.io.IOException;
import java.util.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.asany.jfantasy.graphql.client.GraphQLResponse;
import net.asany.jfantasy.graphql.client.GraphQLTemplate;
import net.asany.jfantasy.graphql.gateway.GraphQLTemplateFactory;
import net.asany.jfantasy.graphql.gateway.data.ServiceDataFetcherFactory;
import net.asany.jfantasy.graphql.gateway.type.ScalarTypeResolver;
import net.asany.jfantasy.graphql.gateway.util.GraphQLTypeUtils;
import org.springframework.http.HttpHeaders;

@Slf4j
@Data
@Builder(builderClassName = "Builder")
public class RemoteGraphQLService implements GraphQLService {
  private String name;
  private String url;

  private Map<String, String> introspectionHeaders;
  private Map<String, String> headers;

  @Setter private Document document;

  private Map<String, Set<String>> excludeFields;

  private TypeResolver defaultTypeResolver;

  private DataFetcherFactory<?> defaultDataFetcher;
  @Getter private ScalarTypeResolver scalarTypeResolver;

  private GraphQLTemplateFactory clientFactory;
  private GraphQLTemplate client;
  private GraphQLSchema schema;

  public Document introspectionQuery() throws IOException {
    GraphQLTemplate client = getClient();

    if (this.introspectionHeaders != null) {
      HttpHeaders headers = new HttpHeaders();
      this.introspectionHeaders.forEach(headers::add);
      client = client.withHeaders(headers);
    }

    String introspectionQuery =
        IntrospectionQueryBuilder.build(
            IntrospectionQueryBuilder.Options.defaultOptions()
                .inputValueDeprecation(false)
                .directiveIsRepeatable(false)
                .isOneOf(false));

    System.out.println("introspectionQuery:" + introspectionQuery);

    GraphQLResponse response = client.post(introspectionQuery, "IntrospectionQuery");

    //noinspection unchecked
    Map<String, Object> introspectionResult = response.get("$.data", HashMap.class);
    return new IntrospectionResultToSchema().createSchemaDefinition(introspectionResult);
  }

  public GraphQLTemplate getClient() {
    if (this.client == null) {
      this.client = clientFactory.client(this);
    }
    return this.client;
  }

  public DataFetcherFactory<?> getDefaultDataFetcher() {
    if (this.defaultDataFetcher == null) {
      this.defaultDataFetcher = new ServiceDataFetcherFactory(this);
    }
    return defaultDataFetcher;
  }

  public TypeResolver getDefaultTypeResolver() {
    if (this.defaultTypeResolver == null) {
      this.defaultTypeResolver = new GraphQLServiceTypeResolver(this);
    }
    return this.defaultTypeResolver;
  }

  public GraphQLSchema getSchema() {
    if (this.schema == null) {
      try {
        this.schema = this.makeSchema();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return this.schema;
  }

  public GraphQLSchema makeSchema() throws IOException {
    this.document = this.introspectionQuery();

    SchemaPrinter.Options noDirectivesOption = defaultOptions().includeDirectives(false);

    TypeDefinitionRegistry typeRegistry =
        new SchemaParser().parse(new SchemaPrinter(noDirectivesOption).print(this.document));

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();

    Map<String, ScalarTypeDefinition> scalars = typeRegistry.scalars();

    for (String scalarName : scalars.keySet()) {
      if (GraphQLTypeUtils.hasBaseScalar(scalarName)) {
        continue;
      }
      runtimeWiringBuilder.scalar(scalarTypeResolver.resolveScalarType(scalarName));
    }

    List<InterfaceTypeDefinition> interfaceTypes =
        typeRegistry.getTypes(InterfaceTypeDefinition.class);
    for (InterfaceTypeDefinition interfaceType : interfaceTypes) {
      runtimeWiringBuilder.type(
          TypeRuntimeWiring.newTypeWiring(interfaceType.getName())
              .typeResolver(this.getDefaultTypeResolver()));
    }

    GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
    codeRegistryBuilder.defaultDataFetcher(this.getDefaultDataFetcher());
    runtimeWiringBuilder.codeRegistry(codeRegistryBuilder.build());

    for (Map.Entry<String, Set<String>> entry : this.excludeFields.entrySet()) {
      String typeName = entry.getKey();
      Set<String> fields = entry.getValue();

      Optional<ObjectTypeDefinition> typeDefinitionOptional =
          typeRegistry.getType(typeName, ObjectTypeDefinition.class);
      if (typeDefinitionOptional.isEmpty()) {
        continue;
      }
      ObjectTypeDefinition typeDefinition = typeDefinitionOptional.get();
      ObjectTypeDefinition.Builder newTypeDefinitionBuilder =
          ObjectTypeDefinition.newObjectTypeDefinition()
              .name(typeDefinition.getName())
              .description(typeDefinition.getDescription())
              .implementz(typeDefinition.getImplements())
              .directives(typeDefinition.getDirectives())
              .fieldDefinitions(
                  typeDefinition.getFieldDefinitions().stream()
                      .filter(fieldDefinition -> !fields.contains(fieldDefinition.getName()))
                      .toList());

      typeRegistry.remove(typeDefinition);
      typeRegistry.add(newTypeDefinitionBuilder.build());
    }

    return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiringBuilder.build());
  }

  public static class Builder {

    public Builder() {
      this.excludeFields = new HashMap<>();
    }

    public Builder addIntrospectionHeader(String name, String value) {
      if (introspectionHeaders == null) {
        introspectionHeaders = new HashMap<>();
      }
      introspectionHeaders.put(name, value);
      return this;
    }

    public Builder addHeader(String name, String value) {
      if (headers == null) {
        headers = new HashMap<>();
      }
      headers.put(name, value);
      return this;
    }

    public Builder excludeFields(List<String> fields) {
      if (this.excludeFields == null) {
        this.excludeFields = new HashMap<>();
      }
      for (String field : fields) {
        String typeName = field.split("\\.")[0];
        String fieldName = field.split("\\.")[1];
        if (!this.excludeFields.containsKey(typeName)) {
          this.excludeFields.put(typeName, Set.of(fieldName));
        } else {
          this.excludeFields.get(typeName).add(fieldName);
        }
      }
      return this;
    }

    public Builder excludeFields(String... fields) {
      return excludeFields(List.of(fields));
    }
  }
}
