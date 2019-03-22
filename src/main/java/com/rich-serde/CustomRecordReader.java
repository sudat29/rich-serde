package serde;


import au.com.bytecode.opencsv.CSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.hive.serde2.OpenCSVSerde;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.util.LineReader;
import org.apache.parquet.hadoop.codec.CompressionCodecNotSupportedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomRecordReader implements RecordReader<LongWritable, Text> {
  private static final Log LOG
    = LogFactory.getLog(CustomRecordReader.class.getName());

  private CompressionCodecFactory compressionCodecs = null;
  private long start;
  private long pos;
  private long end;
  private LineReader in;
  private CSVParser parser;
  private FSDataInputStream fileIn;
  private final Seekable filePosition;
  int maxLineLength;
  private CompressionCodec codec;
  private Decompressor decompressor;
  private static List<String> allTokens = new ArrayList<>();
  private String filedSeparator;
  private String quoteChar;

  public CustomRecordReader(Configuration job, FileSplit split) throws IOException {
    this.maxLineLength = job.getInt(org.apache.hadoop.mapreduce.lib.input.
      LineRecordReader.MAX_LINE_LENGTH, Integer.MAX_VALUE);
    this.filedSeparator = job.get(OpenCSVSerde.SEPARATORCHAR, "\t");
    this.quoteChar = job.get(OpenCSVSerde.QUOTECHAR, "\"");
    start = split.getStart();
    end = start + split.getLength();
    final Path file = split.getPath();
    compressionCodecs = new CompressionCodecFactory(job);
    codec = compressionCodecs.getCodec(file);
    if (codec != null) {
      throw new CompressionCodecNotSupportedException(codec.getClass());
    }

    // open the file and seek to the start of the split
    final FileSystem fs = file.getFileSystem(job);
    fileIn = fs.open(file);
    fileIn.seek(start);
    in = new LineReader(fileIn, job);
    filePosition = fileIn;
    parser = new CSVParser(CSVParser.DEFAULT_SEPARATOR, '\'',
      CSVParser.DEFAULT_ESCAPE_CHARACTER, CSVParser.DEFAULT_STRICT_QUOTES, CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
//    }
    // If this is not the first split, we always throw away first record
    // because we always (except the last split) read one extra line in
    // next() method.
    List<String> tmpToken = new ArrayList<>();
    List<String> tokens;
    boolean isFirstRecord = true;
    long lastSeekIndex = start;
    if (start != 0) {
//      start++;
      in.readLine(new Text(), 0, maxBytesToConsume(pos));
//      int iteration = 0;
//      tokens = new ArrayList<>();
//      while (tokens.size() != 3) {
//        tokens = new ArrayList<>();
//        iteration++;
//        isFirstRecord = iteration == 1;
//        Text first = new Text();
//        lastSeekIndex = start;
//        start += in.readLine(first, maxLineLength, maxBytesToConsume(start));
//        tokens.addAll(Arrays.asList(parser.parseLineMulti(first.toString())));
//        tmpToken.addAll(tokens);
//
//        while(parser.isPending()) {
//          start += in.readLine(first, maxLineLength, maxBytesToConsume(start));
//          tokens.addAll(Arrays.asList(parser.parseLineMulti(first.toString())));
//          tmpToken.addAll(tokens);
//        }
//      }
    }

//    if(!isFirstRecord) {
//      start = lastSeekIndex;
////      fileIn.seek();
//    }
    this.pos = start;
  }

  private int maxBytesToConsume(long pos) {
    return (int) Math.max(Math.min(Integer.MAX_VALUE, end - pos), maxLineLength);
  }

  @Override
  public boolean next(LongWritable key, Text value) throws IOException {

    // We always read one extra line, which lies outside the upper
    // split limit i.e. (end - 1)
    while (pos < end) {
      key.set(pos);

      int newSize = 0;
//      if (pos == 0) {
//        newSize = skipUtfByteOrderMark(value);
//      } else {
      allTokens = new ArrayList<>();
      newSize = in.readLine(value, maxLineLength, maxBytesToConsume(pos));
      String[] tokens = parser.parseLineMulti(value.toString());
      allTokens.addAll(Arrays.asList(tokens));
      pos += newSize;
      while(parser.isPending()) {
        newSize = in.readLine(value, maxLineLength, maxBytesToConsume(pos));
        String[] newTokens = parser.parseLineMulti(value.toString());
        allTokens.addAll(Arrays.asList(newTokens));
        pos += newSize;
      }
//      }
      value.set(quoteChar + String.join(quoteChar+filedSeparator+quoteChar, allTokens) + quoteChar);
      if (newSize == 0) {
        return false;
      }
      if (newSize < maxLineLength) {
        return true;
      }

      // line too long. try again
      LOG.info("Skipped line of size " + newSize + " at pos " + (pos - newSize));
    }

    return false;
  }

  @Override
  public LongWritable createKey() {
    return new LongWritable();
  }

  @Override
  public Text createValue() {
    return new Text();
  }

  @Override
  public long getPos() throws IOException {
    return pos;
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  @Override
  public float getProgress() throws IOException {
    if (start == end) {
      return 0.0f;
    } else {
      return Math.min(1.0f, (pos - start) / (float) (end - start));
    }
  }
}
