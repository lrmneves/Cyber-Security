package randomforestfiles;



import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import com.datastax.driver.core.Row;

import cassandra.CassandraCluster;
import util.ClassificationType;
import util.DataNotLoadedException;
import util.FeatureType;

public class RandomForest implements Serializable {

	static RandomForest currentForest = null;
	ArrayList<DecisionTree> forest ;
	String [] features;//names of the features in header
	String [] labels;//label values from header
	int uniqueLabels;//number of unique labels for the classification task
	int numInstances = -1;//number of instances on training data
	String classificationTask; //attack_cat or attack_or_not classification tasks
	HashMap<String, FeatureType> featureTypesMap; // map from feature to discrete or continuous
	HashMap<String,HashSet<String>> uniqueValueMap;//map to count the number of unique values for each feature 
	//uniqueValueMap is used to det
	ArrayList<Instance >data;
	ArrayList<Instance> currentTestData;
	private int size;
	public String predict(Instance inst){

		HashMap<String,Integer> voteCountMap = new HashMap<>();
		for(DecisionTree tree:forest){
			String pred = tree.predict(inst);
			if(!voteCountMap.containsKey(pred)) voteCountMap.put(pred, 0);
			voteCountMap.put(pred, voteCountMap.get(pred)+ 1);
		}
		int max = -1;
		String bestPred = "";
		for(String key : voteCountMap.keySet()){
			if(voteCountMap.get(key) > max){
				max = voteCountMap.get(key);
				bestPred = key;
			}
		}
		return bestPred;
	}
	//return error
	public double testPrediction(){
		double rightPred = 0;
		double zeroPred = 0;
		for(Instance inst : currentTestData){
			String pred = predict(inst);
			rightPred += pred.equals(inst.getLabels().get(classificationTask))?1:0;
		}
		//		return zeroPred;

		return (double)1.0 - rightPred/currentTestData.size();//returns the error
	}
	//return accuracy
	public double predictLabels(){

		double rightPred = 0.0;
		ArrayList<Instance >data = new ArrayList<>();
		List<Row> rows = CassandraCluster.selectAll("test");
		for(Row r : rows){
			Instance current = loadInstance(r); 
			data.add(current);
		}

		for(Instance inst : data){
			String pred = predict(inst);
			rightPred += pred.equals(inst.getLabels().get(classificationTask))?1:0;
		}		
		return (double) rightPred/data.size();//returns the accuracy
	}
	public RandomForest(){
		this(0);
	}
	public RandomForest(int size){
		features = null;
		labels = null;
		uniqueLabels = 0;
		classificationTask = ClassificationType.ATTACK_OR_NOT;
		uniqueValueMap = new HashMap<>();
		featureTypesMap = new HashMap<>();
		forest = new ArrayList<>();
		this.setSize(size);
		RandomForest.currentForest = this;
	}
	public RandomForest(String type,int size){
		this(size);
		classificationTask = type;
	}
	public void growForest(){
		//		if(forest.size() > 0) forest = new ArrayList<>();
		//		System.out.println("Growing forest");

		//			if(i%(size*0.25) == 0){
		//			System.out.println("Creating tree #" + (i+1));
		//			}
		createTree();
		setSize(getSize() + 1);


	}
	public void addTree(DecisionTree tree){
		forest.add(tree);
		setSize(getSize() + 1);
	}
	public void setSize(int size){
		this.size = size;
	}
	
	public void createTree(){
		Collections.shuffle(data);
		ArrayList<Instance> sampleData = new ArrayList<Instance>(data.subList(0, (int) (numInstances*0.66)));
		currentTestData = new ArrayList<Instance>(data.subList((int) (numInstances*0.66), data.size()));
		
		DecisionTree tree = new DecisionTree(new DecisionTreeNode(
				RandomForestUtils.getSampleFeatures(this.features),sampleData,null));
		tree.getHead().tree = tree;
		try {
			tree.buildTree();
		} catch (DataNotLoadedException e) {
			e.printStackTrace();
		}
		forest.add(tree);
	}

	public void initializeHeaders(){
		List<Row> headers = CassandraCluster.getHeaders();
		features = new String[headers.size()-3];
		labels = new String[2];
		for (Row r : headers){
			int indx = r.getInt("ord");
			if(indx > 0){
				if(indx -1 >= features.length){
					labels[indx-1-features.length] = r.getString("name");

				}
				else{
					features[indx - 1] = r.getString("name");

				}
			}
		}
		for(String f: features){
			uniqueValueMap.put(f,new HashSet<String>());
		}
		for(String l:labels){
			uniqueValueMap.put(l,new HashSet<String>());

		}
		data = new ArrayList<>();
	}

	/**
	 * Load data from csv file path to head node. Does not build tree, just generate instances on head node.
	 * @param csvFile
	 * @param len
	 */
	public void loadData(){


		//initialize features and labels names and unique value maps
		CassandraCluster.startKeyspace();
		initializeHeaders();
		List<Row> rows = CassandraCluster.selectAll();
		for(Row r : rows){
			Instance current = loadInstance(r); 
			data.add(current);
		}

		numInstances = rows.size();
		if(classificationTask.equals(ClassificationType.ATTACK_CAT)){
			uniqueLabels = uniqueValueMap.get(labels[0]).size();
		}else{
			uniqueLabels = uniqueValueMap.get(labels[1]).size();
		}
		for(String f : features){
			if(uniqueValueMap.get(f).size() > 5){
				featureTypesMap.put(f, FeatureType.CONTINUOUS);
			}else{
				featureTypesMap.put(f, FeatureType.DISCRETE);
			}
		}


	}

	/**
	 * Given a line from the dataset, loads instance to Instance object
	 * @param line
	 * @param instanceCount
	 * @return
	 */
	Instance loadInstance(Row r){

		Instance current = new Instance(Integer.parseInt(r.getString("id")),new HashMap<String,String>()
				,new HashMap<String,String>());
		for(int i = 0; i < features.length + labels.length; i++){
			if(i < features.length){
				current.setAttribute(features[i],r.getString(features[i]) != ""?r.getString(features[i]):"NAN");
				if(r.getString(features[i]) != "") uniqueValueMap.get(features[i]).add(r.getString(features[i]));
			}
			else{
				current.setLabel(labels[i-features.length], r.getString(labels[i-features.length]));
			}
		}
		return current;
	}
	//	void pruneForest(int i){
	//		forest = (ArrayList<DecisionTree>) forest.subList(0, i);
	//	}
	public void clearData(){
		data = new ArrayList<>();
		currentTestData = new ArrayList<>();
		for(DecisionTree tree : forest){
			tree.clearData();
		}
	}
	public int getSize() {
		return size;
	}
}
