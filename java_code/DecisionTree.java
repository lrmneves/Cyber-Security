import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
/**
 * Function to load data, initialize tree and run classifier.
 * @author lrmneves
 *
 */
public class DecisionTree implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	DecisionTreeNode head;//tree head
	static String [] features;//names of the features in header
	static int uniqueLabels;//number of unique labels for the classification task
	//	int numInstances = -1;//number of instances on training data
	HashMap<String, FeatureType> featureTypesMap; // map from feature to discrete or continuous
	HashMap<String,HashSet<String>> uniqueValueMap;//map to count the number of unique values for each feature 

	public DecisionTree(DecisionTreeNode head){
		this.head = head;
		head.tree=this;
		featureTypesMap = null;
		uniqueValueMap = null;
	}


	/**
	 * Given an instance, predict its class and returns 1 if prediction was correct and 0 otherwise
	 * @param inst
	 * @param head
	 * @return
	 */
	public String predict(Instance inst) {
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
				if(!splitValue.equals("") &&splitThreshold!=null && !splitThreshold.equals("")&& Double.parseDouble(splitValue) <= Double.parseDouble(splitThreshold)){
					current = current.left;
				}else{
					current = current.right;
				}
			}
		}
		String pred = current.calculateClassLabel();
		//		String trueLabel = inst.getLabels().get(RandomForest.classificationTask);
		//		return pred.equals(trueLabel)?1:0;
		return pred;
	}

	/**
	 * calls the function grwoTree on head to build the tree, but first checks if data has been loaded
	 * @throws DataNotLoadedException
	 */
	public void buildTree() throws DataNotLoadedException{
		this.head.growTree();
		calculateLabels(this.head);
	}
	public void calculateLabels(DecisionTreeNode node){
		node.calculateClassLabel();
		if(node.left!= null) calculateLabels(node.left);
		if(node.right != null) calculateLabels(node.right);
	}
	public void clearData(){
		this.head.clearData();
		uniqueValueMap = null;
		
	}


	public void addInstance(Instance inst) {
		head.addInstance(inst);
	}
	public void setFeatures(String[] features){
		head.setFeatures(features);
	}

	public void createMaps(){
		uniqueValueMap = new HashMap<>();
		featureTypesMap = new HashMap<>();
		for(String f: head.features){
			uniqueValueMap.put(f, new HashSet<String>());
		}
		for(Instance inst : head.data){
			for(String f : uniqueValueMap.keySet()){
				uniqueValueMap.get(f).add(inst.attributes.get(f));
			}
		}
		for(String f : uniqueValueMap.keySet()){
			if(uniqueValueMap.get(f).size() < 5){
				featureTypesMap.put(f, FeatureType.DISCRETE);
			}else{
				featureTypesMap.put(f, FeatureType.CONTINUOUS);
			}
		}

	}





}
