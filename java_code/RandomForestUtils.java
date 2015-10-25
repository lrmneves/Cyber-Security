import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.Row;

public class RandomForestUtils {
	public static final String  CSV_SPLIT_BY = ",";//value to split csv data
	/**
	 * count the lines on a given file
	 * @param path
	 * @return
	 */
	public int countLines(String path){
		int lineCount = -1;
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			lineCount = 0;
			String line = "";

			while ((line = br.readLine()) != null) lineCount++;
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lineCount;
	}
	public static  ArrayList<Instance> loadData(){
		return loadData("test_data");
	}
	public static  ArrayList<Instance> loadData(String path){

		ArrayList<Instance> data = new ArrayList<>();
		List<Row> rows = CassandraCluster.selectAll(path);

		for(Row r : rows){
			data.add(RandomForest.currentForest.loadInstance(r));
		}

		return data;
	}
}	
