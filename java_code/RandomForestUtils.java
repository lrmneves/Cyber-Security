import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
	public static  ArrayList<Instance> loadData(String path){
		BufferedReader br = null;
		String line = "";
		ArrayList<Instance> data = new ArrayList<>();
		try {
			br = new BufferedReader(new FileReader(path));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		try {
			line = br.readLine(); // ignore header
			int count = 0;
			while ((line = br.readLine()) != null) {
				data.add(RandomForest.currentForest.loadInstance(line,count++));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
}	
