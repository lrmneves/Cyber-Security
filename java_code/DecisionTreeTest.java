import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
/**
 * UnitTest to test features and assert Accuracy. Got 76% accuracy on test set using around 50k instances,
 * 40k for training and 10k for test
 * @author lrmneves
 *
 */
public class DecisionTreeTest {
	private DecisionTree tree;
	@Before
    public void initObjects() {
		tree = new DecisionTree();
    }
	@Test(expected = DataNotLoadedException.class) 
	public void BuildTreeTest() throws DataNotLoadedException{
		tree.buildTree();
	}
	@Test
	public void CreateTreeTest() throws DataNotLoadedException {
		tree.loadData("/Users/lrmneves/workspace/Fall 2015/BigData/Cyber-Security/output/train_data.csv");
//		assertEquals(tree.numInstances,576353);
		assertEquals(tree.numInstances,tree.head.data.size());
		
		System.out.println(tree.head.getEntropy().get("label"));
		tree.buildTree();
		System.out.println("Accuracy = "+ tree.predictLabels("/Users/lrmneves/workspace/Fall 2015/BigData/Cyber-Security/output/test_data.csv"));
	}
	

}
