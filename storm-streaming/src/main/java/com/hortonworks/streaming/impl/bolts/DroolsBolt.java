package com.hortonworks.streaming.impl.bolts;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.IRichBolt;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.event.rule.*;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DroolsBolt implements IRichBolt, AgendaEventListener {

  private static final Logger LOG = Logger.getLogger(DroolsBolt.class);
  long alertGapThreshold = 5000;
  private OutputCollector collector;
  private StatefulKnowledgeSession ksession = null;
  private Map<Integer, Long> driverAlertTimes;


  public DroolsBolt() {
    driverAlertTimes = new HashMap<Integer, Long>();
  }


  public void prepare(Map stormConf, TopologyContext context,
                      OutputCollector collector) {

    this.collector = collector;

    try {
      KnowledgeBase kbase = readKnowledgeBase();
      ksession = kbase.newStatefulKnowledgeSession();

      ksession.addEventListener(this);

      ExecutorService executorService = Executors.newFixedThreadPool(1);
      executorService.submit(new Runnable() {
        public void run() {
          try {
            ksession.fireUntilHalt();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }


  }


  public void execute(Tuple input) {

    LOG.info("Entered drools bolt execute...");
    String eventType = input.getStringByField("eventType");
    String driverName = input.getStringByField("driverName");
    String routeName = input.getStringByField("routeName");
    int truckId = input.getIntegerByField("truckId");
    Timestamp eventTime = (Timestamp) input.getValueByField("eventTime");
    double longitude = input.getDoubleByField("longitude");
    double latitude = input.getDoubleByField("latitude");
    int driverId = input.getIntegerByField("driverId");
    SimpleDateFormat sdf = new SimpleDateFormat();

    TruckEvent event = new TruckEvent(driverId, driverName, routeName, truckId, sdf.format(new Date(eventTime.getTime
        ())), eventType, longitude, latitude, false);

    ksession.insert(event);

    emit(event);

    //acknowledge even if there is an error
    collector.ack(input);


  }

  public void emit(TruckEvent event) {
    LOG.info("Entered drool bolt emit...");
    SimpleDateFormat sdf = new SimpleDateFormat();


    collector.emit(new Values(
        event.eventType,
        event.driverName,
        event.routeName,
        event.driverId,
        event.truckId,
        event.eventTime,
        event.longitude,
        event.latitude,
        event.raiseAlert
    ));


  }


  private KnowledgeBase readKnowledgeBase() throws Exception {
    KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    kbuilder.add(ResourceFactory.newClassPathResource("ViolationsRules.drl"), ResourceType.DRL);
    KnowledgeBuilderErrors errors = kbuilder.getErrors();
    if (errors.size() > 0) {
      for (KnowledgeBuilderError error : errors) {
        System.err.println(error);
      }
      throw new IllegalArgumentException("Could not parse knowledge.");
    }
    KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
    kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
    return kbase;
  }


  /**
   * We don't need to set any configuration because at deployment time, it should pick up all configuration from
   * hbase-site.xml
   * as long as it in classpath. Note that we store hbase-site.xml in src/main/resources so it will be in the
   * topology jar that gets deployed
   *
   * @return
   */


  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("eventType", "driverName",
        "routeName", "driverId", "truckId", "timeStamp",
        "longitude", "latitude", "raiseAlert"));
  }


  public Map<String, Object> getComponentConfiguration() {
    return null;
  }


  public void cleanup() {
  }

  public void activationCancelled(ActivationCancelledEvent arg0) {
    // TODO Auto-generated method stub

  }


  public void activationCreated(ActivationCreatedEvent arg0) {
    // TODO Auto-generated method stub

  }

  public void afterActivationFired(AfterActivationFiredEvent arg0) {
    // TODO Auto-generated method stub
    System.out.println("entered afterActivationFired");

    List objects = arg0.getActivation().getObjects();


    for (Object obj : objects) {
      if (obj instanceof TruckEvent) {

        TruckEvent event = (TruckEvent) obj;

        // drools will fire for all matching tuples in a sliding window, even if they
        // are for the same driver. We handle that condition here by not raising an extra alert
        // if a violation event was sent recently (as defined by alertGapThreshold).
        // TODO: figure out if there is a better way to handle this within drools
        int driverid = event.getDriverId();
        if (driverAlertTimes.containsKey(driverid)) {
          long lastViolationAlert = driverAlertTimes.get(driverid);
          if (!((System.currentTimeMillis() - lastViolationAlert) > alertGapThreshold))
            return;

        }


        event.setRaiseAlert(true);


        System.out.println("in afterActivationFired loop, driverid is " + event.getDriverId());

        emit(event);

        driverAlertTimes.put(driverid, System.currentTimeMillis());

        break;

      }
    }


  }

  public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent arg0) {
    // TODO Auto-generated method stub

  }

  public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent arg0) {
    // TODO Auto-generated method stub

  }

  public void agendaGroupPopped(AgendaGroupPoppedEvent arg0) {
    // TODO Auto-generated method stub

  }

  public void agendaGroupPushed(AgendaGroupPushedEvent arg0) {
    // TODO Auto-generated method stub

  }

  public void beforeActivationFired(BeforeActivationFiredEvent arg0) {
    // TODO Auto-generated method stub

  }

  public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent arg0) {
    // TODO Auto-generated method stub

  }

  public void beforeRuleFlowGroupDeactivated(
      RuleFlowGroupDeactivatedEvent arg0) {
    // TODO Auto-generated method stub

  }


}




