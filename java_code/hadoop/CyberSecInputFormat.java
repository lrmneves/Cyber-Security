package hadoop;
//import org.apache.cassandra.hadoop.ColumnFamilyInputFormat;
//import org.apache.cassandra.hadoop.ColumnFamilyRecordReader;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.*;
//
//import org.apache.cassandra.db.IColumn;
//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.mapred.JobConf;
//import org.apache.hadoop.mapred.Reporter;
//import org.apache.hadoop.mapreduce.*;
//
//public class CyberSecInputFormat extends ColumnFamilyInputFormat{
//	@Override
//	public RecordReader<ByteBuffer, SortedMap<ByteBuffer, IColumn>> createRecordReader(InputSplit inputSplit, TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException
//    {
//		  ColumnFamilyRecordReader recordReader = new ColumnFamilyRecordReader();
//		return recordReader;
//		  
//    }
//    public org.apache.hadoop.mapred.RecordReader<ByteBuffer, SortedMap<ByteBuffer, IColumn>> getRecordReader(org.apache.hadoop.mapred.InputSplit split, JobConf jobConf, final Reporter reporter) throws IOException
//    {
//        ColumnFamilyRecordReader recordReader = (ColumnFamilyRecordReader) super.getRecordReader(split, jobConf, reporter);
//        
//        return recordReader;
//    }
//}
