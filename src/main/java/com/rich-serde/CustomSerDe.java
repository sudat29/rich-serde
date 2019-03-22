package serde;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.serde2.AbstractSerDe;
import org.apache.hadoop.hive.serde2.OpenCSVSerde;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeStats;
import org.apache.hadoop.hive.serde2.lazy.LazySerDeParameters;
import org.apache.hadoop.hive.serde.serdeConstants;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.io.Writable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CustomSerDe extends AbstractSerDe {
  private static final String DEFAULT_FIELD_DELIM = "\t";
  private static final String DEFAULT_RECORD_DELIM = "\n";
//  private static final String FIELD_DELIM_KEY = "field.delim";
//  private static final String RECORD_DELIM_KEY = "record.delim";
//  private static final String COLUMNS_KEY = "columns";
//  private static final String COLUMNS_KEY = "columns";

  private String fieldDelim;
  private String recordDelim;
  private List<String> colNames = new ArrayList<>();
  private OpenCSVSerde csvSerde = new OpenCSVSerde();

  @Override
  public void initialize(@Nullable Configuration conf, Properties tbl) throws SerDeException {
    csvSerde.initialize(conf, tbl);
//    LazySerDeParameters serDeParameters = new LazySerDeParameters(conf, tbl, getClass().getName());
//
//    System.out.println("=========");
//    fieldDelim = tbl.getProperty(serdeConstants.FIELD_DELIM, DEFAULT_FIELD_DELIM);
//    recordDelim = tbl.getProperty(serdeConstants.LINE_DELIM, DEFAULT_RECORD_DELIM);
  }

//  @Override
//  public Class<? extends Writable> getSerializedClass() {
//    return null;
//  }
//
//  @Override
//  public Writable serialize(Object obj, ObjectInspector objInspector) throws SerDeException {
//    return null;
//  }
//
//  @Override
//  public SerDeStats getSerDeStats() {
//    return null;
//  }
//
//  @Override
//  public Object deserialize(Writable blob) throws SerDeException {
//    return null;
//  }
//
//  @Override
//  public ObjectInspector getObjectInspector() throws SerDeException {
//    return null;
//  }

  @Override
  public Class<? extends Writable> getSerializedClass() {
    return csvSerde.getSerializedClass();
  }

  @Override
  public Writable serialize(Object obj, ObjectInspector objInspector) throws SerDeException {
    return csvSerde.serialize(obj, objInspector);
  }

  @Override
  public SerDeStats getSerDeStats() {
    return csvSerde.getSerDeStats();
  }

  @Override
  public Object deserialize(Writable blob) throws SerDeException {
    return csvSerde.deserialize(blob);
  }

  @Override
  public ObjectInspector getObjectInspector() throws SerDeException {
    return csvSerde.getObjectInspector();
  }
}
