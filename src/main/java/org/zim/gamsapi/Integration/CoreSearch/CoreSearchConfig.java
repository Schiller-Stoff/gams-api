package org.zim.gamsapi.Integration.CoreSearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.zim.gamsapi.System.configproperties.GAMSDockerDNS;

@Configuration
@EnableElasticsearchRepositories
@RequiredArgsConstructor
@Slf4j
public class CoreSearchConfig extends ElasticsearchConfiguration {

  private final GAMSDockerDNS gamsDockerDNS;

  @Override
  public ClientConfiguration clientConfiguration() {

    if(gamsDockerDNS.getElasticsearchUrl().contains("https://")){
      String msg = String.format("Configured Elasticsearch URL '%s' should not contain 'https://'. Please remove it.", gamsDockerDNS.getElasticsearchUrl());
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }

    if(!gamsDockerDNS.getElasticsearchUrl().contains("http://")){
      String msg = String.format("Configured Elasticsearch URL '%s' should contain 'http://'. Please make sure to pass in a valid URL.", gamsDockerDNS.getElasticsearchUrl());
      log.error(msg);
      throw new IllegalArgumentException(msg);
    }

    // Extract the host address from the Elasticsearch URL
    String elasticsearchHostAddress = gamsDockerDNS.getElasticsearchUrl().replace("http://", "");

    // https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/clients.html#elasticsearch.clients.configuration
    return ClientConfiguration.builder()
        .connectedTo(
            elasticsearchHostAddress
        )
        .build();
  }

}

