import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
/**
 * Function to load data, initialize tree and run classifier.
 * @author lrmneves
 *
 */
public class DecisionTree {
	DecisionTreeNode head;//tree head
	final String  cvsSplitBy = ",";//value to split csv data
	static String [] features;//names of the features in header
	static HashMap<String, FeatureType> featureTypesMap; // map from feature to discrete or continuous
	static String [] labels;//label values from header
	static int uniqueLabels;//number of unique labels for the classification task
	int numInstances = -1;//number of instances on training data
	static String classificationTask; //attack_cat or attack_or_not classification tasks
	static HashMap<String,HashSet<String>> uniqueValueMap;//map to count the number of unique values for each feature 
	//uniqueValueMap is used to determine if feature is discrete or continuous or if task is binary or multiclass
	public DecisionTree(String classification){
		this();
		DecisionTree.classificationTask = (classification.toLowerCase().equals("attack_cat"))? ClassificationType.ATTACK_CAT:
			ClassificationType.ATTACK_OR_NOT;
	}
	public DecisionTree(){
		head = new DecisionTreeNode();
		features = null;
		labels = null;
		uniqueLabels = 0;
		DecisionTree.classificationTask = ClassificationType.ATTACK_OR_NOT;
		uniqueValueMap = new HashMap<>();
		featureTypesMap = new HashMap<>();
	}
	public void loadData(String path){
		loadData(path,-1);
	}
	/**
	 * Loads test set and runs predict on all instances, calculating the accuracy of the model
	 * @param path
	 * @return
	 */
	public double predictLabels(String path){
		BufferedReader br = null;
		String line = "";
		int rightPred = 0;
		ArrayList<Instance >data = new ArrayList<>();
		try {
			br = new BufferedReader(new FileReader(path));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		try {
			line = br.readLine(); // ignore header
			int count = 0;
			while ((line = br.readLine()) != null) {
				data.add(loadInstance(line,count++));
			}
			
			for(Instance inst : data){
				rightPred += predict(inst,head);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (double) rightPred/data.size();//returns the accuracy
	}
	/**
	 * Given an instance, predict its class and returns 1 if prediction was correct and 0 otherwise
	 * @param inst
	 * @param head
	 * @return
	 */
	private int predict(Instance inst, DecisionTreeNode head) {
		DecisionTreeNode current = head;
		
		while(!current.isLeaf()){
			String splitFeature = current.futureSplitFeature;
			String splitValue = inst.getAttributes().get(splitFeature);
			String splitThreshold = current.left != null? current.left.pastSplitFeatureValue:null;
			
			if(featureTypesMap.get(splitFeature).equals(FeatureType.DISCRETE)){
				if(splitValue.equals(splitThreshold)){
					current = current.left;
				}else{
					current = current.right;
				}
			}else{
				if(!splitValue.equals("") && Double.parseDouble(splitValue) <= Double.parseDouble(splitThreshold)){
					current = current.left;
				}else{
					current = current.right;
				}
			}
		}
		String pred = current.calculateClassLabel();
		String trueLabel = inst.getLabels().get(DecisionTree.classificationTask);
		return pred.equals(trueLabel)?1:0;
	}
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
	/**
	 * calls the function grwoTree on head to build the tree, but first checks if data has been loaded
	 * @throws DataNotLoadedException
	 */
	public void buildTree() throws DataNotLoadedException{
		if(numInstances < 0) throw new DataNotLoadedException("Load data before building tree");
		this.head.growTree(features);

	}
	/**
	 * Given a line from the dataset, loads instance to Instance object
	 * @param line
	 * @param instanceCount
	 * @return
	 */
	Instance loadInstance(String line,int instanceCount){
		String[] instance = line.split(cvsSplitBy);
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
	/**
	 * Load data from csv file path to head node. Does not build tree, just generate instances on head node.
	 * @param csvFile
	 * @param len
	 */
	public void loadData(String csvFile,int len){

		BufferedReader br = null;
		String line = "";

		ArrayList<Instance >data = null;
		if(len < 0){
			len = countLines(csvFile) -1;
			this.numInstances = len;
		}
		try {

			br = new BufferedReader(new FileReader(csvFile));
			int instanceCount = 0;
			while ((line = br.readLine()) != null) {
				//initialize features and labels names and unique value maps
				if(features == null){
					String [] allValues = line.split(cvsSplitBy);
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
				else{

					Instance current = loadInstance(line,instanceCount); 
					data.add(current);
					instanceCount++;
				}
			}
			head.setData(data);
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




}
