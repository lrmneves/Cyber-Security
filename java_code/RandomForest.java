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
import java.util.Random;

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
	int size;
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
		
		for(Instance inst : currentTestData){
			String pred = predict(inst);
			rightPred += pred.equals(inst.getLabels().get(classificationTask))?1:0;
		}
//		return zeroPred;
		
		return (double)1.0 - rightPred/currentTestData.size();//returns the error
	}
	//return accuracy
	public double predictLabels(String path){
//		System.out.println("Predicting Labels");
		int rightPred = 0;
		ArrayList<Instance >data = RandomForestUtils.loadData(path);
		for(Instance inst : data){
			String pred = predict(inst);
			rightPred += pred.equals(inst.getLabels().get(classificationTask))?1:0;
		}		
		return (double) rightPred/data.size();//returns the accuracy
	}
	public RandomForest(){
		this(100);
	}
	public RandomForest(int size){
		features = null;
		labels = null;
		uniqueLabels = 0;
		classificationTask = ClassificationType.ATTACK_OR_NOT;
		uniqueValueMap = new HashMap<>();
		featureTypesMap = new HashMap<>();
		forest = new ArrayList<>();
		this.size = size;
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
		
		
	}
	public void setSize(int size){
		this.size = size;
	}
	public void createTree(){
		Collections.shuffle(data);
		ArrayList<Instance> sampleData = new ArrayList<Instance>(data.subList(0, (int) (numInstances*0.66)));
		currentTestData = new ArrayList<Instance>(data.subList((int) (numInstances*0.66), data.size()));
		String [] sampleFeatures = new String[(int) Math.sqrt(features.length)];
		HashSet<Integer> repeated = new HashSet<>();
		repeated.add(0);//ignore row count column
		for(int i = 0; i < sampleFeatures.length;i++){
			int idx = new Random().nextInt(features.length);
			while(repeated.contains(idx)){
				idx = new Random().nextInt(features.length);
			}
			repeated.add(idx);
			sampleFeatures[i] = features[idx];
		}
		DecisionTree tree = new DecisionTree(new DecisionTreeNode(sampleFeatures,sampleData));
		try {
			tree.buildTree();
		} catch (DataNotLoadedException e) {
			e.printStackTrace();
		}
		forest.add(tree);
	}

	public void initializeHeaders(String line){
		String [] allValues = line.split(RandomForestUtils.CSV_SPLIT_BY);
		features = Arrays.copyOfRange(allValues,1,allValues.length-2);
		for(String f: features){
			uniqueValueMap.put(f,new HashSet<String>());
		}
		labels = Arrays.copyOfRange(allValues,allValues.length-2,allValues.length);
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
	public void loadData(String csvFile){

		BufferedReader br = null;
		String line = "";

		try {
			br = new BufferedReader(new FileReader(csvFile));
			int instanceCount = 0;
			while ((line = br.readLine()) != null) {
				//initialize features and labels names and unique value maps
				if(features == null){
					initializeHeaders(line);
				}
				else{
					Instance current = loadInstance(line,instanceCount); 
					data.add(current);
					instanceCount++;
				}
			}
			numInstances = instanceCount;
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

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Given a line from the dataset, loads instance to Instance object
	 * @param line
	 * @param instanceCount
	 * @return
	 */
	 Instance loadInstance(String line,int instanceCount){
		String[] instance = line.split(RandomForestUtils.CSV_SPLIT_BY);
		Instance current = new Instance(instanceCount,new HashMap<String,String>()
				,new HashMap<String,String>());
		for(int i = 0; i < features.length + labels.length; i++){
			if(i < features.length){
				current.setAttribute(features[i],(instance[i+1] != "")? instance[i+1]:"NAN");
				if(instance[i+1] != "") uniqueValueMap.get(features[i]).add(instance[i+1]);
			}
			else{
				current.setLabel(labels[i-features.length], instance[i+1]);
			}
		}
		return current;
	}
//	void pruneForest(int i){
//		forest = (ArrayList<DecisionTree>) forest.subList(0, i);
//	}
	void clearData(){
		data = new ArrayList<>();
		currentTestData = new ArrayList<>();
		for(DecisionTree tree : forest){
			tree.clearData();
		}
	}
}
