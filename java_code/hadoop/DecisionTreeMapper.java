package hadoop;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.StringTokenizer;

import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import randomforestfiles.Instance;

import org.apache.cassandra.db.Cell;




class DecisionTreeMapper extends  Mapper<ByteBuffer, SortedMap<ByteBuffer, Cell>,IntWritable, Text> {
	private Logger logger = Logger.getLogger(DecisionTreeMapper.class);


	protected void setup(Context context)
			throws IOException, InterruptedException
	{
	}
	public String removeAllSpecial(String str){
		return str.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").replaceAll("\f", "").trim();
	}
	public Instance createInstance(SortedMap<ByteBuffer, Cell> columns) throws CharacterCodingException{
		
		HashMap<String,String> attributes = new HashMap<>();
		HashMap<String,String> labels = new HashMap<>();

		for (Cell cell : columns.values())
		{
			String name  = removeAllSpecial(ByteBufferUtil.string(cell.name().toByteBuffer()));
			String value = removeAllSpecial(ByteBufferUtil.string(cell.value()));
			
			if(name.equals("type") && value.trim().equals("test")) return null;
			if(name.equals("label") || name.equals("attack_cat")){
				labels.put(name, value);
			}
			else if (!name.trim().isEmpty()){
				attributes.put(name,value);
				}
		}
		return new Instance(1,attributes,labels);
	}
	public void map(ByteBuffer key, SortedMap<ByteBuffer, Cell> columns, Context context)throws IOException, InterruptedException{
		Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.setPrettyPrinting()
				.serializeNulls()
				.create();
		Random generator = new Random();
		int idx = generator.nextInt(context.getConfiguration().getInt("trees", 10));

		Instance inst = createInstance(columns);
		if (inst != null) context.write(new IntWritable(idx),new Text(gson.toJson(inst)));
	}

}
