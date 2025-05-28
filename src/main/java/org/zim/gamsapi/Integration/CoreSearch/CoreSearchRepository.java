package org.zim.gamsapi.Integration.CoreSearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CoreSearchRepository extends ElasticsearchRepository<CoreSearchEntity, String> {



}
