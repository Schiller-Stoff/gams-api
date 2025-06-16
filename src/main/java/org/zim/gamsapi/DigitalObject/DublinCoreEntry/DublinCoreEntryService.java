package org.zim.gamsapi.DigitalObject.DublinCoreEntry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DublinCoreEntryService {

  private final IDublinCoreEntryRepository dublinCoreEntryRepository;

  @Transactional
  public Page<DublinCoreEntryDTO> findAll(MultiValueMap<String, String> filters, Pageable pageable) {

    String msg = String.format(
        "Finding all DublinCoreEntries with filters: %s, pageable: %s",
        filters, pageable
    );
    log.debug(msg);

    var foundEntries = dublinCoreEntryRepository.findAll(
        new DCGenericSpecification<>(filters), pageable);


    log.debug("Found {} DublinCoreEntries", foundEntries.getTotalElements());


    // TODO switch from dto to projection?
    return foundEntries.map(dublinCoreEntrySummaryView -> {
      return new DublinCoreEntryDTO(
          dublinCoreEntrySummaryView.getName(),
          dublinCoreEntrySummaryView.getValue(),
          dublinCoreEntrySummaryView.getLanguage(),
          dublinCoreEntrySummaryView.getDigitalObject().getId());
    });
  }

}
