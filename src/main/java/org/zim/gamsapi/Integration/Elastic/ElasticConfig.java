package org.zim.gamsapi.Integration.Elastic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.erhlc.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
public class ElasticConfig extends ElasticsearchConfiguration {

  @Override
  public ClientConfiguration clientConfiguration() {

    // https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/clients.html#elasticsearch.clients.configuration
    return ClientConfiguration.builder()
        // TODO remove hardcoded value
        .connectedTo("localhost:9200")
        .build();
  }

}

