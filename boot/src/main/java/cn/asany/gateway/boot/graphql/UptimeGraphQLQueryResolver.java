package cn.asany.gateway.boot.graphql;

import graphql.kickstart.tools.GraphQLQueryResolver;
import java.lang.management.ManagementFactory;
import org.springframework.stereotype.Component;

@Component
public class UptimeGraphQLQueryResolver implements GraphQLQueryResolver {

  public String uptime() {
    long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
    long hours = uptimeMillis / 3600_000;
    long minutes = (uptimeMillis % 3600_000) / 60_000;
    long seconds = (uptimeMillis % 60_000) / 1000;
    return hours + "h " + minutes + "m " + seconds + "s";
  }
}
