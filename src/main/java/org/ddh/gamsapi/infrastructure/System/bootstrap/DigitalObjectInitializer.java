package org.ddh.gamsapi.infrastructure.System.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@Slf4j
@RequiredArgsConstructor
@Component
public class DigitalObjectInitializer implements CommandLineRunner {

  @Override
  public void run(String... args) {
    log.info("*** Start bootstrapping gams-api ...");
    logAvailableSystemResources();
  }

  /**
   * Info logs available system resources.
   */
  private void logAvailableSystemResources(){

    int mb = 1024 * 1024;
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
    long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
    log.info("*** GAMS-API Initialization: Initial Memory (xms) : {}mb", xms);
    log.info("*** GAMS-API Initialization: Max Memory (xmx) : {}mb", xmx);
  }

}
