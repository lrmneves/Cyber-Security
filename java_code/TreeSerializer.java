import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

//import java.io.File;
//import org.apache.avro.Schema;
//import org.apache.avro.file.DataFileWriter;
//import org.apache.avro.file.DataFileReader;
//import org.apache.avro.file.CodecFactory;
//import org.apache.avro.io.DatumWriter;
//import org.apache.avro.io.DatumReader;
//import org.apache.avro.reflect.ReflectData;
//import org.apache.avro.reflect.ReflectDatumWriter;
//import org.apache.avro.reflect.ReflectDatumReader;
//import org.apache.avro.reflect.Nullable;

public class TreeSerializer {
	static void purgeDirectory(File dir) {
	    for (File file: dir.listFiles()) {
	        if (file.isDirectory()) purgeDirectory(file);
	        file.delete();
	    }
	}
	
	static String serializeTree(RandomForest forest, String path){
		return serializeTree(forest,path,"forest.json");
	}

	
	static String serializeTree(RandomForest forest, String path, String fileName){
		Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.setPrettyPrinting()
				.serializeNulls()
				.create();
		
		purgeDirectory(new File(path));


		PrintWriter writer = null;
		forest.clearData();
		
		try {
			writer = new PrintWriter(path +fileName , "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.write(gson.toJson(forest));
		writer.close();
		return gson.toJson(forest);
	}
	static RandomForest openTree(String path){
		Gson gson = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
				.setPrettyPrinting()
				.serializeNulls()
				.create();
		
		String json = "";
		try {
			json = deserializeString(new File(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		RandomForest forest = gson.fromJson(json,RandomForest.class);

		return forest;
	}
	public static String deserializeString(File file)
			  throws IOException {
			      int len;
			      char[] chr = new char[4096];
			      final StringBuffer buffer = new StringBuffer();
			      final FileReader reader = new FileReader(file);
			      try {
			          while ((len = reader.read(chr)) > 0) {
			              buffer.append(chr, 0, len);
			          }
			      } finally {
			          reader.close();
			      }
			      return buffer.toString();
			  }

}
