import java.io.Serializable;

/**
 * Object to hold important values during information gain calculation.
 * @author lrmneves
 *
 */
public class InformationGainObject implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	double informationGain; //information gain for this split
	DecisionTreeNode left;//left child if split using this feature
	DecisionTreeNode right;//right child if split using this feature
	int featureIdx;
	
	public InformationGainObject(double informationGain, DecisionTreeNode left,DecisionTreeNode right,int idx){
		this.informationGain = informationGain;
		this.left = left;
		this.right = right;
		this.featureIdx = idx;
	}
}
