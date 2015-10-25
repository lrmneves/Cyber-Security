import java.io.Serializable;
import java.util.ArrayList;
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
	
	public DecisionTree(DecisionTreeNode head){
		this.head = head;
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
			
			if(RandomForest.currentForest.featureTypesMap.get(splitFeature).equals(FeatureType.DISCRETE)){
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
		
	}
	public void clearData(){
		this.head.clearData();
	}
	
	
	




}
