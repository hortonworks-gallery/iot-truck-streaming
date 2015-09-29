package com.hortonworks.streaming.impl.bolts.hive;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.processors.CommandProcessorResponse;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.apache.log4j.Logger;
import org.apache.storm.hdfs.common.rotation.RotationAction;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BackupHiveTablePartitionAction implements RotationAction {


  private static final long serialVersionUID = 2725320320183384402L;

  private static final Logger LOG = Logger.getLogger(BackupHiveTablePartitionAction.class);
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private String sourceMetastoreUrl;
  private String databaseName;
  private String tableName;
  private String sourceFSrl;


  public BackupHiveTablePartitionAction(String sourceMetastoreUrl, String tableName, String databaseName, String
      sourceFSUrl) {
    super();
    this.sourceMetastoreUrl = sourceMetastoreUrl;
    this.tableName = tableName;
    this.databaseName = databaseName;
    this.sourceFSrl = sourceFSUrl;
  }

  @Override
  public void execute(FileSystem fileSystem, Path filePath)
      throws IOException {

    long timeStampFromFile = getTimestamp(filePath.getName());
    Date date = new Date(timeStampFromFile);


    String datePartitionName = constructDatePartitionName(date);
    String hourPartitionName = constructHourPartitionName(date);

    String fileNameWithSchema = sourceFSrl + filePath.toString();

    LOG.info("About to add file[" + fileNameWithSchema + "] to a partitions[date=" + datePartitionName + ", hour=" +
        hourPartitionName + "]");
    addFileToPartition(fileNameWithSchema, datePartitionName, hourPartitionName);

  }

  private String constructHourPartitionName(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    return String.valueOf(hour);
  }

  private String constructDatePartitionName(Date date) {
    return dateFormat.format(date);
  }

  private long getTimestamp(String fileName) {
    int startIndex = fileName.lastIndexOf("-");
    int endIndex = fileName.lastIndexOf(".");
    String timeStamp = fileName.substring(startIndex + 1, endIndex);
    return Long.valueOf(timeStamp);
  }

  private void addFileToPartition(String fileNameWithSchema, String datePartitionName, String hourPartitionName) {

    LOG.info("adding datePartition[" + datePartitionName + "], hourPartition[" + hourPartitionName + "]");

    loadData(fileNameWithSchema, datePartitionName, hourPartitionName);

  }

  public void loadData(String path, String datePartitionName, String hourPartitionName) {

    StringBuilder ddl = new StringBuilder();
    ddl.append(" load data inpath ")
        .append(" '").append(path).append("' ")
        .append(" into table ")
        .append(tableName)
        .append(" partition ").append(" (eventDate='").append(datePartitionName).append("', hour='").append
        (hourPartitionName).append("')");

    startSessionState(sourceMetastoreUrl);
    try {
      execHiveDDL("use " + databaseName);
      execHiveDDL(ddl.toString());
    } catch (Exception e) {
      String errorMessage = "Error exexcuting query[" + ddl.toString() + "]";
      LOG.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  public void startSessionState(String metaStoreUrl) {
    HiveConf hcatConf = new HiveConf();
    hcatConf.setVar(HiveConf.ConfVars.METASTOREURIS, metaStoreUrl);
    hcatConf.set("hive.metastore.local", "false");
    hcatConf.set(HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY.varname, "false");
    hcatConf.set("hive.root.logger", "DEBUG,console");
    SessionState.start(hcatConf);
  }

  public void execHiveDDL(String ddl) throws Exception {
    LOG.info("Executing ddl = " + ddl);

    Driver hiveDriver = new Driver();
    CommandProcessorResponse response = hiveDriver.run(ddl);

    if (response.getResponseCode() > 0) {
      throw new Exception(response.getErrorMessage());
    }
  }

}
