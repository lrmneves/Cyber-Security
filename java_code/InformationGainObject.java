/**
 * Object to hold important values during information gain calculation.
 * @author lrmneves
 *
 */
public class InformationGainObject {
	double informationGain; //information gain for this split
	DecisionTreeNode left;//left child if split using this feature
	DecisionTreeNode right;//right child if split using this feature
	
	public InformationGainObject(double informationGain, DecisionTreeNode left,DecisionTreeNode right){
		this.informationGain = informationGain;
		this.left = left;
		this.right = right;
	}
}
