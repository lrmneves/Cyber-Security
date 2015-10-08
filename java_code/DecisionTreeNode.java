import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class DecisionTreeNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	String [] features;
	DecisionTreeNode left;
	DecisionTreeNode right;
	ArrayList<Instance> data;//instances under this node.
	HashMap<String,Double> entropyValueMap = null;//Entropy value for each feature and label. 
	String futureSplitFeature = ""; //value which divide the data into two child. 
	String pastSplitFeatureValue =""; // the threshold of previous split. Only left child has this value set
	String classificationLabel = ""; //label to be predicted to an instance who ends up here. Only leaf nodes have this
	//value set.


	public DecisionTreeNode(String [] features, ArrayList<Instance> data){
		left = null;
		right = null;
		this.data = data;
		if (this.data ==null) this.data = new ArrayList<Instance>();
		this.features = features;
	}

	/**
	 * Calculate the entropy of this node for every feature and for the label
	 * @return HashMap key -> label or feature, value -> entropy
	 */
	public HashMap<String,Double> getEntropy(){
		if(entropyValueMap != null) return entropyValueMap;
		//calculate the entropy for all features on this node
		HashMap<String,HashMap<String,Integer>> countMap = new HashMap<>();
		//all features will have arraylists of counts for each label

		countMap.put(RandomForest.currentForest.classificationTask, new HashMap<String,Integer>());
		//return map with all calculated entropies 
		HashMap<String,Double> entropyMap = new HashMap<>();
		//for each instance in data, increment the feature count for their label.
		for(Instance inst : data){
			//get label related to the classification task
			String label = inst.getLabels().get(RandomForest.currentForest.classificationTask);

			if(!countMap.get(RandomForest.currentForest.classificationTask).containsKey(label)) countMap.get(
					RandomForest.currentForest.classificationTask).put(label, 0);
			countMap.get(RandomForest.currentForest.classificationTask).put(label, countMap.get(RandomForest.currentForest.classificationTask).get(label) +1);
		}
		//calculate the entropy for each feature
		for(String f : countMap.keySet()){
			double entropy = 0;
			for(String l: countMap.get(f).keySet()){
				double probability = (double) countMap.get(f).get(l)/data.size();
				entropy+= -probability *log2(probability);
			}
			entropyMap.put(f, entropy);
		}
		entropyValueMap = entropyMap;
		return entropyMap;
	}
	/**
	 * calculate log2 of value
	 * @param value
	 * @return
	 */
	public double log2(double value){
		return Math.log10(value)/Math.log10(2);
	}
	/**
	 * Separate the data into two child nodes given a feature.
	 * @param feature
	 * @return
	 */
	public DecisionTreeNode [] getChildren(final String feature){
		//calculate values for left and right child
		DecisionTreeNode [] children = new DecisionTreeNode[2];
		DecisionTreeNode left = null;
		DecisionTreeNode right = null;

		FeatureType type = RandomForest.currentForest.featureTypesMap.get(feature);
		Iterator<String> it = RandomForest.currentForest.uniqueValueMap.get(feature).iterator();
		//if discrete value, work on separating them into two groups
		if(type.equals(FeatureType.DISCRETE)){
			//If values are discrete, test all combinations of separating all values to one child and all other vlaues
			//to different child and get the split with max entropy
			double minEntropy = Integer.MAX_VALUE;
			//Separates all values of a group on a child and all others to the other child.
			while(it.hasNext()){
				String v1 = it.next();
				DecisionTreeNode auxLeft = new DecisionTreeNode(features,null);
				DecisionTreeNode auxRight = new DecisionTreeNode(features,null);
				for(Instance inst: data){
					if(inst.getAttributes().get(feature).equals(v1)){
						auxLeft.addInstance(inst);
					}else{
						auxRight.addInstance(inst);
					}
				}
				if(auxLeft.getEntropy().get(RandomForest.currentForest.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
						auxRight.getEntropy().get(RandomForest.currentForest.classificationTask)*
						((double)auxRight.data.size()/data.size()) < minEntropy ){
					left = auxLeft;
					left.pastSplitFeatureValue = v1;
					right = auxRight;
				}
				if(RandomForest.currentForest.uniqueValueMap.get(feature).size() <3) break;
			}
		}else{
			//If values are continuous, sort values and look for the best split using binary search.
			//Sort values
			Collections.sort(data,new Comparator<Instance>(){
				public int compare(Instance i1,Instance i2){
					return i1.getAttributes().get(feature).compareTo(i2.getAttributes().get(feature));
				}});
			DecisionTreeNode auxLeft; 
			DecisionTreeNode auxRight;
			int start = 0;
			int end = data.size();
			int mid = (start+end)/2;
			double variation = Integer.MAX_VALUE;
			double minEntropy = Integer.MAX_VALUE;
			auxLeft = new DecisionTreeNode(features,null);
			auxRight = new DecisionTreeNode(features,null);

			auxLeft.data.addAll(data.subList(0, mid));
			auxRight.data.addAll(data.subList(mid, end));
			minEntropy = auxLeft.getEntropy().get(RandomForest.currentForest.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
					auxRight.getEntropy().get(RandomForest.currentForest.classificationTask)*
					((double)auxRight.data.size()/data.size());
			left = auxLeft;
			right = auxRight;
			auxRight.addInstance(auxLeft.data.remove(auxLeft.data.size()-1));

			double auxEntropy = auxLeft.getEntropy().get(RandomForest.currentForest.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
					auxRight.getEntropy().get(RandomForest.currentForest.classificationTask)*
					((double)auxRight.data.size()/data.size());

			if(minEntropy < auxEntropy){
				start = mid;
			}else{
				end = mid-1;
				minEntropy = auxEntropy; 
			}
			while(start <= end && variation > 0.01){
				mid = (start + end)/2;

				auxLeft = new DecisionTreeNode(features,null);
				auxRight = new DecisionTreeNode(features,null);
				auxLeft.data.addAll(data.subList(0, mid));
				auxRight.data.addAll(data.subList(mid, data.size()));

				auxEntropy = auxLeft.getEntropy().get(RandomForest.currentForest.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
						auxRight.getEntropy().get(RandomForest.currentForest.classificationTask)*
						((double)auxRight.data.size()/data.size());

				variation =(double) Math.abs(minEntropy - auxEntropy);
				if(minEntropy < auxEntropy){
					start = mid+1;
				}else{
					end = mid-1;
					minEntropy = auxEntropy; 
					left = auxLeft;
					left.pastSplitFeatureValue = data.get(mid).getAttributes().get(feature);
					right = auxRight;
				}

			}

		}
		children[0] = left;
		children[1] = right;
		return children;
	}

	/**
	 * Calculate information gain of a feature, being it the 
	 * (Entropy of this node - Weighted average of entropy of child nodes) as described on 
	 * http://homes.cs.washington.edu/~shapiro/EE596/notes/InfoGain.pdf
	 * @param features
	 * @return
	 * @throws DataNotLoadedException
	 */
	public InformationGainObject calculateInformationGain() throws DataNotLoadedException{
		double max = Integer.MIN_VALUE;
		InformationGainObject obj = null;
		double informationGain;
		boolean gotLoop = false;
		boolean gotComp = false;
		ArrayList<Double> list = new ArrayList<>();
		for(int i = 0; i < features.length;i++){
			DecisionTreeNode [] children = getChildren(features[i]);
			DecisionTreeNode left = children[0];
			DecisionTreeNode right = children[1];

			informationGain = this.getEntropy().get(RandomForest.currentForest.classificationTask) - 
					(((double) left.data.size()/data.size()) * left.getEntropy().get(RandomForest.currentForest.classificationTask) +
							((double)right.data.size()/data.size())* right.getEntropy().get(RandomForest.currentForest.classificationTask));
			list.add(informationGain);
			if(informationGain > max){
				obj = new InformationGainObject(informationGain,left,right,i);
			}
		}
		if(obj == null){
			System.out.println();
		}
		return obj;
	}
	/**
	 * Run a dfs on growing a tree, calculating the split and generating the new nodes.
	 * @param features
	 * @throws DataNotLoadedException
	 */
	public void growTree() throws DataNotLoadedException{

		Stack<DecisionTreeNode> stack = new Stack<>();
		stack.push(this);
		DecisionTreeNode current;
		while(!stack.isEmpty()){
			current = stack.pop();
			HashSet<String> labelSet = new HashSet<>();
			for(Instance ins : current.data){
				labelSet.add(ins.getLabels().get(RandomForest.currentForest.classificationTask));
			}

			if(labelSet.size()<2) continue;
			
			InformationGainObject maxGain = current.calculateInformationGain();

			if(maxGain != null && maxGain.left.data.size() > 0 && maxGain.informationGain > 0.1){
				current.left = maxGain.left;
				stack.push(current.left);
			}
			else current.left = null;
			if(maxGain != null && maxGain.right.data.size() > 0 && maxGain.informationGain > 0.1){
				current.right = maxGain.right;
				stack.push(current.right);
			}
			else current.right = null;
			if (current.isLeaf()){
				current.calculateClassLabel();
			}
			if(maxGain != null)current.futureSplitFeature = features[maxGain.featureIdx];
		}

	}
	/**
	 * Calculate the most representative class on that node, to be used by the classifier
	 * @return
	 */
	public String calculateClassLabel(){
		if (!classificationLabel.equals("")) return classificationLabel;
		HashMap<String,Integer> countMap = new HashMap<>();

		for(Instance i : data){
			String label = i.labels.get(RandomForest.currentForest.classificationTask);
			if(!countMap.containsKey(label)){
				countMap.put(label, 0);
			}
			countMap.put(label,countMap.get(label)+ 1);
		}
		int max = 0;
		String maxLabel = "";
		for(String l : countMap.keySet()){
			if(countMap.get(l) > max){
				max = countMap.get(l);
				maxLabel = l;
			}
		}
		classificationLabel = maxLabel;
		return classificationLabel;
	}
	public DecisionTreeNode(int id){
		left = null;
		right = null;
		data = new ArrayList<>();
	}
	public DecisionTreeNode(){
		this(0);
	}

	public DecisionTreeNode getLeft() {
		return left;
	}


	public void setLeft(DecisionTreeNode left) {
		this.left = left;
	}


	public DecisionTreeNode getRight() {
		return right;
	}


	public void setRight(DecisionTreeNode right) {
		this.right = right;
	}
	public ArrayList<Instance> getData() {
		return data;
	}
	public void setData(ArrayList<Instance> data) {
		this.data = data;
	}
	public void addInstance(Instance i){
		if(data == null) data = new ArrayList<>();
		this.data.add(i);
	}
	public boolean isLeaf(){
		return left == null && right == null;
	}
	public int getH(){
		if(isLeaf()) return 0;
		return Math.max(left.getH(), right.getH()) +1;
	}

	public void clearData() {
		this.data = new ArrayList<Instance>();
		
		if(left != null)left.clearData();
		if(right != null) right.clearData();

	}

}
