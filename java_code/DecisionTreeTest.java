import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
/**
 * UnitTest to test features and assert Accuracy. Got 76% accuracy on test set using around 50k instances,
 * 40k for training and 10k for test
 * @author lrmneves
 *
 */
public class DecisionTreeTest {
	private RandomForest forest;
	final String projectDir = "/Users/lrmneves/workspace/Fall 2015/BigData/Cyber-Security/";
	final String dataDir = projectDir + "output/";
	final String modelDir = projectDir +  "model/";
	@Before
	public void initObjects() {

		forest = new RandomForest(1);
	}
//	public double std(ArrayList<Double> list){
//		double mean = mean(list);
//		for( int i = 0; i < list.size();i++){
//			list.set(i, Math.pow(list.get(i) - mean, 2));
//		}
//		double variance = mean(list);
//		return Math.sqrt(variance);
//	}
//	public double mean(ArrayList<Double> list){
//		double sum = 0;
//		for(double n : list){
//			sum+=n;
//		}
//		return sum/list.size();
//	}
//	@Test
//	public void CreateTreeTest() throws DataNotLoadedException {
//	
//		forest.loadData(dataDir + "train_data.csv");
//
//		System.out.println("Loaded Data");
//		int N = 200;
//		ArrayList<Double> list = new ArrayList<>();
//
//		for(int i = 0; i < N; i++){
//			forest.growForest();
//			double error = forest.testPrediction();
//			list.add(error);
//		}
//		System.out.println("Grew forest and saved error values");
//
//		StringBuilder builder = new StringBuilder();
//		builder.append("Trees,Error(%)").append("\n");
//		int idx = 0;
//		double min = Integer.MAX_VALUE;
//		for(int i = 1; i < list.size()+1;i++){
////			if(list.get(i-1) < min){
////				min = list.get(i-1);
////				idx = i;
////			}
//			builder.append(i + ","+list.get(i-1)).append("\n");
//		}
//		TreeSerializer.serializeTree(forest, modelDir);
////		forest.pruneForest(idx);
////		TreeSerializer.serializeTree(forest, modelDir,"bestModel.json");
//
//		System.out.println("Wrote Json");
//
//		try {
//			PrintWriter writer = new PrintWriter(modelDir + "error.csv", "UTF-8");
//			writer.write(builder.toString());
//			System.out.println("Wrote error.csv");
//			writer.close();
//		} catch (FileNotFoundException | UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//	}
//	
	@Test
	public void reuseModelTest(){
		forest = TreeSerializer.openTree(modelDir+"forest.json");
//		RandomForest bestModel =  TreeSerializer.openTree(modelDir+"bestModel.json");
		System.out.println("Read Json");
		System.out.println(forest.predictLabels(dataDir + "test_data.csv"));
//		forest = new RandomForest();
//		System.out.println(bestModel.predictLabels(dataDir + "test_data.csv"));
	}


}
