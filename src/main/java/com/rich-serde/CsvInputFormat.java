package serde;

import au.com.bytecode.opencsv.CSVParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.LineReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvInputFormat
//  extends TextInputFormat {
  extends FileInputFormat<LongWritable, Text>
  implements JobConfigurable {
  private int N = 1;
//  private CSVParser parser;

  public CsvInputFormat() {

  }

  public RecordReader<LongWritable, Text> getRecordReader(
    InputSplit genericSplit,
    JobConf job,
    Reporter reporter)
    throws IOException {
    reporter.setStatus(genericSplit.toString());
    return new CustomRecordReader(job, (FileSplit) genericSplit);
  }

  /**
   * Logically splits the set of input files for the job, splits N lines
   * of the input as one split.
   *
   * @see org.apache.hadoop.mapred.FileInputFormat#getSplits(JobConf, int)
   */
  public InputSplit[] getSplits(JobConf job, int numSplits)
    throws IOException {
    ArrayList<FileSplit> splits = new ArrayList<>();
    for (FileStatus status : listStatus(job)) {
//      for (FileSplit split :
//        getSplitsForFile(status, job, N)) {
      splits.addAll(getSplitsForFile(status, job, N));
//      }
    }
    return splits.toArray(new FileSplit[splits.size()]);
  }

  public void configure(JobConf conf) {
    N = 2;
//      conf.getInt("mapreduce.input.lineinputformat.linespermap", 1);
  }

  /**
   * NLineInputFormat uses LineRecordReader, which always reads
   * (and consumes) at least one character out of its upper split
   * boundary. So to make sure that each mapper gets N lines, we
   * move back the upper split limits of each split
   * by one character here.
   * @param fileName  Path of file
   * @param begin  the position of the first byte in the file to process
   * @param length  number of bytes in InputSplit
   * @return  FileSplit
   */
  static FileSplit createFileSplit(Path fileName, long begin, long length) {
    return (begin == 0)
      ? new FileSplit(fileName, begin, length - 1, new String[] {})
      : new FileSplit(fileName, begin - 1, length, new String[] {});
  }

  public List<FileSplit> getSplitsForFile(FileStatus status,
                                                 Configuration conf, int numLinesPerSplit) throws IOException {
    CSVParser parser = new CSVParser(CSVParser.DEFAULT_SEPARATOR, '\'',
      CSVParser.DEFAULT_ESCAPE_CHARACTER, CSVParser.DEFAULT_STRICT_QUOTES, CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
    List<FileSplit> splits = new ArrayList<FileSplit> ();
    Path fileName = status.getPath();
    if (status.isDirectory()) {
      throw new IOException("Not a file: " + fileName);
    }
    List<String> tokens = new ArrayList<>();
    FileSystem fs = fileName.getFileSystem(conf);
    LineReader lr = null;
    try {
      FSDataInputStream in  = fs.open(fileName);
      lr = new LineReader(in, conf);
      Text line = new Text();
      int numLines = 0;
      long begin = 0;
      long length = 0;
      int num = -1;
      while ((num = lr.readLine(line)) > 0) {
        length += num;
        tokens.addAll(Arrays.asList(parser.parseLineMulti(line.toString())));
        if(parser.isPending()) {
          continue;
        }
        numLines++;
        if (numLines == numLinesPerSplit) {
          splits.add(createFileSplit(fileName, begin, length));
          begin += length;
          length = 0;
          numLines = 0;
        }
      }
      if (numLines != 0) {
        splits.add(createFileSplit(fileName, begin, length));
      }
    } finally {
      if (lr != null) {
        lr.close();
      }
    }
    return splits;
  }

}

//  @Override
//  public RecordReader<LongWritable, Text> getRecordReader(InputSplit genericSplit, JobConf job, Reporter reporter)
//    throws IOException {
//    reporter.setStatus(genericSplit.toString());
//    String delimiter = job.get("textinputformat.record.delimiter");
//    byte[] recordDelimiterBytes = null;
//    if (null != delimiter) {
//      recordDelimiterBytes = delimiter.getBytes(Charsets.UTF_8);
//    }
//    return new CustomRecordReader(job, (FileSplit) genericSplit,
//      recordDelimiterBytes);
//
//  }
//
//}
