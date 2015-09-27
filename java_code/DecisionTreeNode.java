import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class DecisionTreeNode {
	int id;
	DecisionTreeNode left;
	DecisionTreeNode right;
	ArrayList<Instance> data;//instances under this node.
	HashMap<String,Double> entropyValueMap = null;//Entropy value for each feature and label. 
	String futureSplitFeature = ""; //value which divide the data into two child. 
	String pastSplitFeatureValue =""; // the threshold of previous split. Only left child has this value set
	String classificationLabel = ""; //label to be predicted to an instance who ends up here. Only leaf nodes have this
	//value set.
	/**
	 * Calculate the entropy of this node for every feature and for the label
	 * @return HashMap key -> label or feature, value -> entropy
	 */
	public HashMap<String,Double> getEntropy(){
		if(entropyValueMap != null) return entropyValueMap;
		//calculate the entropy for all features on this node
		HashMap<String,HashMap<String,Integer>> countMap = new HashMap<>();
		//all features will have arraylists of counts for each label
		for(String f: DecisionTree.features){
			countMap.put(f, new HashMap<String,Integer>());
		}
		countMap.put(DecisionTree.classificationTask, new HashMap<String,Integer>());
		//return map with all calculated entropies 
		HashMap<String,Double> entropyMap = new HashMap<>();
		//for each instance in data, increment the feature count for their label.
		for(Instance inst : data){
			//get label related to the classification task
			String label = inst.getLabels().get(DecisionTree.classificationTask);
			//count each label for each feature
			//			for(String f: DecisionTree.features){
			//				if(!countMap.get(f).containsKey(label)) countMap.get(f).put(label,0); 
			//				countMap.get(f).put(label, countMap.get(f).get(label) + 1);
			//			}
			if(!countMap.get(DecisionTree.classificationTask).containsKey(label)) countMap.get(
					DecisionTree.classificationTask).put(label, 0);
			countMap.get(DecisionTree.classificationTask).put(label, countMap.get(DecisionTree.classificationTask).get(label) +1);
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

		FeatureType type = DecisionTree.featureTypesMap.get(feature);
		Iterator<String> it = DecisionTree.uniqueValueMap.get(feature).iterator();
		//if discrete value, work on separating them into two groups
		if(type.equals(FeatureType.DISCRETE)){
			//If values are discrete, test all combinations of separating all values to one child and all other vlaues
			//to different child and get the split with max entropy
			double minEntropy = Integer.MAX_VALUE;
			//Separates all values of a group on a child and all others to the other child.
			while(it.hasNext()){
				String v1 = it.next();
				DecisionTreeNode auxLeft = new DecisionTreeNode();
				DecisionTreeNode auxRight = new DecisionTreeNode();
				for(Instance inst: data){
					if(inst.getAttributes().get(feature).equals(v1)){
						auxLeft.addInstance(inst);
					}else{
						auxRight.addInstance(inst);
					}
				}
				if(auxLeft.getEntropy().get(DecisionTree.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
						auxRight.getEntropy().get(DecisionTree.classificationTask)*
						((double)auxRight.data.size()/data.size()) < minEntropy ){
					left = auxLeft;
					left.pastSplitFeatureValue = v1;
					right = auxRight;
				}
				if(DecisionTree.uniqueValueMap.get(feature).size() <3) break;
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
			auxLeft = new DecisionTreeNode();
			auxRight = new DecisionTreeNode();
			
			auxLeft.data.addAll(data.subList(0, mid));
			auxRight.data.addAll(data.subList(mid, end));
			minEntropy = auxLeft.getEntropy().get(DecisionTree.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
					auxRight.getEntropy().get(DecisionTree.classificationTask)*
					((double)auxRight.data.size()/data.size());
			
			auxRight.addInstance(auxLeft.data.remove(auxLeft.data.size()-1));
			
			double auxEntropy = auxLeft.getEntropy().get(DecisionTree.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
					auxRight.getEntropy().get(DecisionTree.classificationTask)*
					((double)auxRight.data.size()/data.size());
			
			if(minEntropy < auxEntropy){
				start = mid;
			}else{
				end = mid-1;
				minEntropy = auxEntropy; 
			}
			while(start <= end && variation > 0.05 && minEntropy > 0.1){
				mid = (start + end)/2;
				
				auxLeft = new DecisionTreeNode();
				auxRight = new DecisionTreeNode();
				auxLeft.data.addAll(data.subList(start, mid));
				auxRight.data.addAll(data.subList(mid, end));
				
				auxEntropy = auxLeft.getEntropy().get(DecisionTree.classificationTask)*((double)auxLeft.data.size()/data.size()) + 
						auxRight.getEntropy().get(DecisionTree.classificationTask)*
						((double)auxRight.data.size()/data.size());
				
				variation =(double) Math.abs(minEntropy - auxEntropy);
				if(minEntropy < auxEntropy){
					start = mid;
				}else{
					end = mid-1;
					minEntropy = auxEntropy; 
				}
				
			}
			left = auxLeft;
			left.pastSplitFeatureValue = data.get(mid).getAttributes().get(feature);
			right = auxRight;
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
	public InformationGainObject [] calculateInformationGain(String [] features) throws DataNotLoadedException{
		InformationGainObject [] informationGainFeature = new InformationGainObject[features.length];
		for(int i = 0; i < features.length;i++){
			DecisionTreeNode [] children = getChildren(features[i]);
			DecisionTreeNode left = children[0];
			DecisionTreeNode right = children[1];

			double informationGain = this.getEntropy().get(DecisionTree.classificationTask) - 
					(((double) left.data.size()/data.size()) * left.getEntropy().get(DecisionTree.classificationTask) +
							((double)right.data.size()/data.size())* right.getEntropy().get(DecisionTree.classificationTask));
			informationGainFeature[i] =  new InformationGainObject(informationGain,left,right);
		}
		return informationGainFeature;
	}
	/**
	 * Run a dfs on growing a tree, calculating the split and generating the new nodes.
	 * @param features
	 * @throws DataNotLoadedException
	 */
	public void growTree(String [] features) throws DataNotLoadedException{

		Stack<DecisionTreeNode> stack = new Stack<>();
		stack.push(this);
		DecisionTreeNode current;
		while(!stack.isEmpty()){
			current = stack.pop();
			HashSet<String> labelSet = new HashSet<>();
			for(Instance ins : current.data){
				labelSet.add(ins.getLabels().get(DecisionTree.classificationTask));
			}

			if(labelSet.size()<2) continue;

			InformationGainObject [] gain = current.calculateInformationGain(features);
			double max = Double.MIN_VALUE;
			int max_indx = 0;
			for(int i = 0; i < gain.length;i++){
				if(max < gain[i].informationGain){
					max = gain[i].informationGain;
					max_indx = i;
				}
			}
			if(gain[max_indx].left.data.size() > 0 && gain[max_indx].informationGain > 0.1){
				current.left = gain[max_indx].left;
				stack.push(current.left);
			}
			else current.left = null;
			if(gain[max_indx].right.data.size() > 0 && gain[max_indx].informationGain > 0.1){
				current.right = gain[max_indx].right;
				stack.push(current.right);
			}
			else current.right = null;
			if (current.isLeaf()){
				current.calculateClassLabel();
			}
			current.futureSplitFeature = DecisionTree.features[max_indx];
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
			String label = i.labels.get(DecisionTree.classificationTask);
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
		this.id = id;
		left = null;
		right = null;
		data = new ArrayList<>();
	}
	public DecisionTreeNode(){
		this(0);
	}

	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
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

}
