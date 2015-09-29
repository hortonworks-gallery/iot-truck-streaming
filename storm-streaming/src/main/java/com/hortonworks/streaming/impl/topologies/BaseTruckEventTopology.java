package com.hortonworks.streaming.impl.topologies;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public abstract class BaseTruckEventTopology {
  private static final Logger LOG = Logger.getLogger(BaseTruckEventTopology.class);
  protected Properties topologyConfig;

  public BaseTruckEventTopology(String configFileLocation) throws Exception {
    topologyConfig = new Properties();
    try {
      topologyConfig.load(new FileInputStream(configFileLocation));
    } catch (FileNotFoundException e) {
      LOG.error("Encountered error while reading configuration properties: "
          + e.getMessage());
      throw e;
    } catch (IOException e) {
      LOG.error("Encountered error while reading configuration properties: "
          + e.getMessage());
      throw e;
    }
  }
}
