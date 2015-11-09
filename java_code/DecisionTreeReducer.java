import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.Logger;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.hadoop.io.Text;
public class DecisionTreeReducer extends Reducer<IntWritable , Text, ByteBuffer, List<Mutation>>{
	Logger logger = Logger.getLogger(DecisionTreeReducer.class);



	public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException{


		DecisionTree tree = new DecisionTree(new DecisionTreeNode());
		Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.setPrettyPrinting()
				.serializeNulls()
				.create();
		
        for (Text val : values){
        	String value = val.toString();
    		tree.addInstance((Instance) gson.fromJson(value,Instance.class));
        }
       
		int featuresSize = tree.head.data.get(0).attributes.keySet().size();
		String [] features = new String[featuresSize];
		int idx = 0;
		for (String f : tree.head.data.get(0).attributes.keySet()){
			features[idx++] = f;
			
		}
		tree.setFeatures(RandomForestUtils.getSampleFeatures(features));
		try {
			tree.buildTree();
		} catch (DataNotLoadedException e) {
			e.printStackTrace();
		}
		
		tree.clearData();
	
		String treeJson = gson.toJson(tree);
		
		
		List<Mutation> v = Collections.singletonList(getMutation(treeJson));
		context.write(ByteBufferUtil.bytes(key.get()), v);

	} 
	
	  private static Mutation getMutation(String tree)
      {	  Text col = new Text("tree");
      	  Text tree_text = new Text(tree);
          org.apache.cassandra.thrift.Column c = new org.apache.cassandra.thrift.Column();
//          c.setName(ByteBufferUtil.bytes("tree"));
          c.setName(Arrays.copyOf(col.getBytes(), col.getLength()));

          c.setValue(Arrays.copyOf(tree_text.getBytes(), tree_text.getLength()));
          c.setTimestamp(System.currentTimeMillis());

          Mutation m = new Mutation();
     
          m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
          m.column_or_supercolumn.setColumn(c);
          return m;
      }
	
//	private Map<String, ByteBuffer> keys;
//    private ByteBuffer key;
//    protected void setup(org.apache.hadoop.mapreduce.Reducer.Context context)
//    throws IOException, InterruptedException
//    {
//        keys = new LinkedHashMap<String, ByteBuffer>();
//    }
//
//    public void reduce(IntWritable word, Iterable<Text> values, Context context) throws IOException, InterruptedException
//    {
//        int sum = 0;
//        for (IntWritable val : values)
//            sum += val.get();
//       
//
//    private List<ByteBuffer> getBindVariables(Text word, int sum)
//    {
//        List<ByteBuffer> variables = new ArrayList<ByteBuffer>();
//        variables.add(ByteBufferUtil.bytes(String.valueOf(sum)));         
//        return variables;
//    }
//	private static Mutation getMutation(Text word, int sum)
//	{
//		org.apache.cassandra.thrift.Column c = new org.apache.cassandra.thrift.Column();
//		c.setName(Arrays.copyOf(word.getBytes(), word.getLength()));
//		c.setValue(ByteBufferUtil.bytes(sum));
//		c.setTimestamp(System.currentTimeMillis());
//
//		Mutation m = new Mutation();
//		m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
//		m.column_or_supercolumn.setColumn(c);
//		return m;
//	}
//
//		private static Mutation getMutation(String name, String value, boolean isVal){
//			Column col = new Column();
//			col.setName(ByteBufferUtil.bytes(name));
//			if(isVal) col.setValue(ByteBufferUtil.bytes(1));
//			else col.setValue(ByteBufferUtil.bytes(value));
//			col.setTimestamp(System.currentTimeMillis());
//			Mutation mutation = new Mutation();
//			mutation.setColumn_or_supercolumn(new
//					ColumnOrSuperColumn());
//			mutation.getColumn_or_supercolumn().setColumn(col);
//			return mutation;
//	
//		}
}
