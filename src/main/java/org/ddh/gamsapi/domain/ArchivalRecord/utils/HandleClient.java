package org.ddh.gamsapi.domain.ArchivalRecord.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;

@Component
@Slf4j
public class HandleClient implements IHandleClient {

  @Override
  public String generate() {
    return HandleGenerator.generate();
  }


  @Override
  public void register(String pid, URI target) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void retarget(String pid, URI target) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Optional<URI> resolveTarget(String pid) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean exists(String pid) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void delete(String pid) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
