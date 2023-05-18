package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatastreamService implements IDatastreamService {

  private final IDatastreamRepository datastreamRepository;
  @Override
  @Transactional
  public void delete(Datastream datastream) {
    datastreamRepository.delete(datastream);
  }

  @Override
  @Transactional
  public void delete(DigitalObject digitalObject, String dsid) {
    datastreamRepository.deleteByDigitalObjectAndDsid(digitalObject, dsid);
  }


  @Override
  public Datastream findById(Long id) throws DatastreamNotFoundException {
    return datastreamRepository.findById(id).orElseThrow(() -> {
      String msg = String.format("Cannot find datastream with id %s", id);
      log.info(msg);
      return new DatastreamNotFoundException(msg);
    });
  }

  @Override
  public Datastream findByDsid(String pid, String dsid) throws DatastreamNotFoundException {
    DigitalObject digitalObject = DigitalObject.builder().pid(pid).build();
    return datastreamRepository.findByDigitalObjectAndDsid(digitalObject, dsid).orElseThrow(() -> {
      String msg = String.format("Cannot find datastreams via pid %s and dsid %s", pid, dsid);
      log.info(msg);
      return new DatastreamNotFoundException(msg);
    });
  }


  @Override
  @Transactional
  public Datastream save(Datastream datastream) {
    return datastreamRepository.save(datastream);
  }
}
