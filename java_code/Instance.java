import java.util.HashMap;
/**
 * Instance object hold the values for each attribute and the respective labels for attack_cat and attack_or_not
 * @author lrmneves
 *
 */
public class Instance {
	//all the attributes
	HashMap<String,String> attributes;
	//labels[0] = attack_cat, labels[1] = "0" for normal, "1" for attack
	HashMap<String,String> labels;
	int id;
	
	public Instance(int id, HashMap<String,String> attributes,  HashMap<String,String> labels){
		this.id = id;
		this.attributes = attributes;
		this.labels = labels;
	}

	public  HashMap<String,String> getAttributes() {
		return attributes;
	}

	public void setAttribute(String feature, String value) {
		this.attributes.put(feature,value);
	}

	public  HashMap<String,String> getLabels() {
		return labels;
	}

	public void setLabel(String label, String value) {
		this.labels.put(label, value);
	}
	
	
}

