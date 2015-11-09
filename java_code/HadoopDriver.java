import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.*;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Row;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.hadoop.io.Text;
import org.apache.cassandra.hadoop.*;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.hadoop.cql3.CqlConfigHelper;
import org.apache.cassandra.hadoop.cql3.CqlInputFormat;
import org.apache.cassandra.hadoop.cql3.CqlOutputFormat;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.commons.lang.ArrayUtils;

public class HadoopDriver{
	private static final Logger logger = LoggerFactory.getLogger(HadoopDriver.class);

	public static void main(String[] args){
		CassandraCluster.connect();
		
		String exp = "exp2";
		if(args.length > 0){
			exp = args[0];
		}

		RandomForest forest = new RandomForest();
		BasicConfigurator.configure();

		Job job = null;
		
		for( int i = 0; i < 2; i++){
			Configuration conf = new Configuration();
			try {
				job = Job.getInstance(conf);
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			job.getConfiguration().set("experiment", exp);

			//input general case
			job.setJarByClass(HadoopDriver.class);

			job.setInputFormatClass(ColumnFamilyInputFormat.class);
			job.setOutputFormatClass(ColumnFamilyOutputFormat.class);

			ConfigHelper.setInputPartitioner(job.getConfiguration(), "Murmur3Partitioner");
			ConfigHelper.setInputRpcPort(job.getConfiguration(), "9160");
			ConfigHelper.setInputInitialAddress(job.getConfiguration(),"localhost");

			ConfigHelper.setOutputPartitioner(job.getConfiguration(), "Murmur3Partitioner");
			ConfigHelper.setOutputInitialAddress(job.getConfiguration(), "localhost");

			ConfigHelper.setInputSplitSize(job.getConfiguration(), 1);
			SliceRange range=new SliceRange(ByteBuffer.wrap(ArrayUtils.EMPTY_BYTE_ARRAY),ByteBuffer.wrap(ArrayUtils.EMPTY_BYTE_ARRAY),false,Integer.MAX_VALUE);
			SlicePredicate predicate=new SlicePredicate().setColumn_names(null).setSlice_range(range);
			ConfigHelper.setInputSlicePredicate(conf,predicate);
			ConfigHelper.setInputSlicePredicate(job.getConfiguration(), predicate);


			if( i == 0){
				//config first pass for decision tree
				//set number of trees
				job.getConfiguration().setInt("trees",5);
				job.setMapperClass(DecisionTreeMapper.class);
				ConfigHelper.setInputColumnFamily(job.getConfiguration(),
						CassandraCluster.getKeyspace(), "cyber_sec");



				//Output Config

				job.setReducerClass(DecisionTreeReducer.class);

				job.setMapOutputKeyClass(IntWritable.class);
				job.setMapOutputValueClass(Text.class);
				job.setOutputKeyClass(ByteBuffer.class);
				job.setOutputValueClass(List.class);


				ConfigHelper.setOutputColumnFamily(job.getConfiguration(),
						CassandraCluster.getKeyspace(), "tree_table");
			}else{
				job.setMapperClass(RandomForestMapper.class);

				ConfigHelper.setInputColumnFamily(job.getConfiguration(),
						CassandraCluster.getKeyspace(), "tree_table");

				job.setReducerClass(RandomForestReducer.class);

				job.setMapOutputKeyClass(IntWritable.class);
				job.setMapOutputValueClass(Text.class);
				job.setOutputKeyClass(ByteBuffer.class);
				job.setOutputValueClass(List.class);


				ConfigHelper.setOutputColumnFamily(job.getConfiguration(),
						CassandraCluster.getKeyspace(), "forest_table");
			}

			try { 
				if(!job.waitForCompletion(true)) throw new Exception("Job didn't complete"); //run the job
			} catch(Exception e) {
				System.err.println("ERROR IN JOB: " + e);
				e.printStackTrace();
				return;
			}
		}
		Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.setPrettyPrinting()
				.serializeNulls()
				.create();
		forest =(RandomForest) gson.fromJson(CassandraCluster.getForestString(exp),RandomForest.class);
		forest.initializeHeaders();
		TreeSerializer.serializeTree(forest, "/Users/lrmneves/workspace/Fall 2015/BigData/Cyber-Security/output/");
		CassandraCluster.persistErrorValues(exp,"test_hadoop",forest.size,1-forest.predictLabels());
		
		CassandraCluster.close();


	}
}
