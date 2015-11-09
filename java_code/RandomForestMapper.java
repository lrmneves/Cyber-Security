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

import org.apache.cassandra.db.Cell;




class RandomForestMapper extends  Mapper<ByteBuffer, SortedMap<ByteBuffer, Cell>,IntWritable, Text> {
	private Logger logger = Logger.getLogger(RandomForestMapper.class);


	protected void setup(Context context)
			throws IOException, InterruptedException
	{
	}
	public String removeAllSpecial(String str){
		return str.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "").replaceAll("\f", "").trim();
	}
	
	public void map(ByteBuffer key, SortedMap<ByteBuffer, Cell> columns, Context context)throws IOException, InterruptedException{
		String tree = "";
		for (Cell cell : columns.values())
		{
			String name  = removeAllSpecial(ByteBufferUtil.string(cell.name().toByteBuffer()));
			String value =ByteBufferUtil.string(cell.value());
			if(name.equals("tree")) tree = value;
		}
		if(tree.length()>0) context.write(new IntWritable(1),new Text(tree));
	}

}
