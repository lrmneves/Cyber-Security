import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
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

		forest = new RandomForest(10);
	}
			@Test
			public void testCreateTable(){
				CassandraCluster.connect();
				try {
					CassandraCluster.createTable(dataDir + "train_data.csv",dataDir +"test_data.csv");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				CassandraCluster.close();
			}
	public double std(ArrayList<Double> list){
		double mean = mean(list);
		for( int i = 0; i < list.size();i++){
			list.set(i, Math.pow(list.get(i) - mean, 2));
		}
		double variance = mean(list);
		return Math.sqrt(variance);
	}
	public double mean(ArrayList<Double> list){
		double sum = 0;
		for(double n : list){
			sum+=n;
		}
		return sum/list.size();
	}

	@Test
	public void CreateTreeTest() throws DataNotLoadedException {

		CassandraCluster.connect();

		forest.loadData();

		System.out.println("Loaded Data");
		int N = 10;
		ArrayList<Double> list = new ArrayList<>();
		String type = "train";
		Date date= new Date();
		long time = date.getTime();
		String exp = type + time;
		for(int i = 0; i < N; i++){
			forest.growForest();
			double error = forest.testPrediction();
			CassandraCluster.persistErrorValues(exp,type,i+1,error);
		}
		System.out.println("Grew forest and saved error values");

		StringBuilder builder = new StringBuilder();
		builder.append("Trees,Error(%)").append("\n");
		int idx = 0;
		double min = Integer.MAX_VALUE;
		for(int i = 1; i < list.size()+1;i++){

			builder.append(i + ","+list.get(i-1)).append("\n");
		}
		TreeSerializer.serializeTree(forest, modelDir);
		//		forest.pruneForest(idx);
		//		TreeSerializer.serializeTree(forest, modelDir,"bestModel.json");

		System.out.println("Wrote Json");


		CassandraCluster.close();

	}


//	@Test
//	public void reuseModelTest(){
//		CassandraCluster.connect();
//
//		CassandraCluster.startKeyspace();
//		forest = TreeSerializer.openTree(modelDir+"forest.json");
//		//		RandomForest bestModel =  TreeSerializer.openTree(modelDir+"bestModel.json");
//		String type = "test";
//		Date date= new Date();
//		long time = date.getTime();
//		String exp = type + time;
//		CassandraCluster.persistErrorValues(exp,type,forest.size,1-forest.predictLabels());
//		
//		CassandraCluster.close();
//
//		//		forest = new RandomForest();
//	}

}
