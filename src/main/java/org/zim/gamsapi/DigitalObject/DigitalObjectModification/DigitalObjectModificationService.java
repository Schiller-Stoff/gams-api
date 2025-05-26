package org.zim.gamsapi.DigitalObject.DigitalObjectModification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zim.gamsapi.Datastream.IDatastreamRepository;
import org.zim.gamsapi.DigitalObject.DigitalObject;
import org.zim.gamsapi.DigitalObject.IDigitalObjectRepository;
import org.zim.gamsapi.DigitalObject.exceptions.DigitalObjectNotFoundException;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalObjectModificationService  implements IDigitalObjectModificationService {

  private final IDigitalObjectRepository digitalObjectRepository;
  private final IDatastreamRepository datastreamRepository;

  public DigitalObjectModification findLatestModificationDate(String digitalObjectId) {

    if(!digitalObjectRepository.existsById(digitalObjectId)){
      String msg = String.format("DigitalObject with id %s does not exist", digitalObjectId);
      log.warn(msg);
      throw new DigitalObjectNotFoundException(msg);
    }

    DigitalObjectModification digitalObjectModification = calculateLatestModificationDate(digitalObjectId);
    log.info("Calculated latest modification date for digital object {} as {}", digitalObjectId, digitalObjectModification);
    return digitalObjectModification;
  }

  public DigitalObjectModification calculateLatestModificationDate(String digitalObjectId) {

    // setup for return
    DigitalObjectModification digitalObjectModification = new DigitalObjectModification();
    digitalObjectModification.setId(digitalObjectId);

    // this could be a more efficient query?
    Optional<DigitalObject> digitalObjectOptional = digitalObjectRepository.findById(digitalObjectId);

    if(digitalObjectOptional.isEmpty()) {
      String msg = String.format("DigitalObject with id %s does not exist", digitalObjectId);
      log.error(msg);
      throw new DigitalObjectNotFoundException(msg);
    }

    DigitalObject foundDigitalObject = digitalObjectOptional.get();

    Optional<Date> datastreamLastModifiedDate = datastreamRepository.findMaxLastModifiedDateByDigitalObjectId(digitalObjectId);

    // return last modified (if no datastreams are available)
    if(datastreamLastModifiedDate.isEmpty()) {
       String msg = String.format("No datastreams found for digital object with id %s", digitalObjectId);
       log.debug(msg);
       digitalObjectModification.setLatestModificationDate(foundDigitalObject.getModified());
       return digitalObjectModification;
    }

    Date lastDigitalObjectModifiedDate = foundDigitalObject.getModified();
    Date lastDatastreamModifiedDate = datastreamLastModifiedDate.get();

    // set the latest modification date
    if(lastDigitalObjectModifiedDate.after(lastDatastreamModifiedDate)) {
      digitalObjectModification.setLatestModificationDate(lastDigitalObjectModifiedDate);
    } else {
      digitalObjectModification.setLatestModificationDate(lastDatastreamModifiedDate);
    }

    return digitalObjectModification;

  }

}
