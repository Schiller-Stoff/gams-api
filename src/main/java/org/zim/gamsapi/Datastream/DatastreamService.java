package org.zim.gamsapi.Datastream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zim.gamsapi.Datastream.exceptions.DatastreamNotFoundException;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamDetailsView;
import org.zim.gamsapi.Datastream.interfaces.IDatastreamService;
import org.zim.gamsapi.DigitalObject.DigitalObject;

import java.util.List;

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
    DigitalObject digitalObject = DigitalObject.builder().id(pid).build();
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

  /**
   * Returns a list of datastream projections based on the parent digital object.
   * The projection excludes the actual datastream content. (to improve performance)
   * @param digitalObject parent digital object
   * @return list of datastream projections
   */
  @Override
  public List<IDatastreamDetailsView> findAll(DigitalObject digitalObject) {
    return datastreamRepository.findAllByDigitalObjectId(digitalObject.getId());
  }

  /**
   * Returns a datastream projection based on the parent digital object, and it's datastream-identifier.
   * The projection excludes the actual datastream content. (to improve performance
   * @param objectId of the parent digital object
   * @param dsid user defined datastream-identifier (must be unique per object)
   * @return found Datastream projection
   * @throws DatastreamNotFoundException if no datastream is found
   */
  @Override
  public IDatastreamDetailsView findDatastreamDetailsByDsid(String objectId, String dsid) throws DatastreamNotFoundException {
    return datastreamRepository.findDatastreamDetailsViewByDigitalObjectAndDsid(DigitalObject.builder().id(objectId).build(), dsid);
  }
}
