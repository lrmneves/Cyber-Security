
// import required spark classes
//import org.apache.spark.mllib.regression.LabeledPoint
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector


import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.tuning.{ParamGridBuilder, CrossValidator}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.Pipeline


import org.apache.spark.mllib.linalg._

import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.DataFrame

import org.apache.spark.ml.feature.VectorAssembler


object CassandraTest {
  /**
   * Recursively cast columns to double. They were stored as strings on previous work.
   */
	def castColumnsToDouble( df: DataFrame,count : Int ) : DataFrame = {
			if (count > 0){
			  if(df.columns.apply(count-1) == "label"){
			    //here is a little hack to fix StringIndexer changing 1 to 0 and 0 to 1. I get my labels and 
			    // do label - 1 * -1, making 0 to be 1 and 1 to be 0.
			    val newDf : DataFrame = df.withColumn( df.columns.apply(count-1), (df(df.columns.apply(count-1))
			        .cast("double") - 1) * -1)
			    return castColumnsToDouble(newDf,count-1)
			  }
			  else{
				  val newDf : DataFrame = df.withColumn( df.columns.apply(count-1), df(df.columns.apply(count-1))
				      .cast("double"))
			   	return castColumnsToDouble(newDf,count-1)
			  }
			}
			else{
				return df
			}
	}
	/**
	 * Get dataset part (train, test)
	 */
	def getDataset(part : String , cc :CassandraSQLContext , keySpace : String ,
	    columnFamily : String) : DataFrame = {
	  
	  return cc.sql("SELECT * from " + keySpace +"." + 
	    columnFamily+" where type = \""+part+"\"")
	    
	}
	/**
	 * Persist error to cassandra
	 */
		def persistError(exp : String ,expType:String, treeNum: Int, error : Double,
		    conf :SparkConf , keySpace : String , columnFamily : String) = {
	   CassandraConnector(conf).withSessionDo {session =>
	     session.execute ( "INSERT INTO "+ keySpace+"."+ columnFamily+"(experiment,type, tree_num, error)" +
   "VALUES('" + exp+"','"+expType+"'," + treeNum + ',' + error+");")
	   }
	    
	}
	/**
	 * Drop the columns that will not be used as features or labels
	 */
	def dropBadColumns(df : DataFrame) : DataFrame = {
	  return df.drop("attack_cat")
	    .drop("type")
	    .drop("id")
	    .toDF()
	}
	def main(args: Array[String]) {
		//initialize variables
		val cHost: String = "localhost"
		val cPort: String = "9042"
		val keySpace = "bigdata_class"
		val inputColumnFamily = "cyber_sec"
		val outputColumnFamily = "result"
		val numTrees = 5
		val numFolds = 2
		// initialize spark context
		val conf = new SparkConf(true)
	    .set("spark.cassandra.connection.host", cHost)
	    .setAppName("RandomForest")
	    .setMaster("local[*]")
	    .set("spark.cassandra.connection.port", cPort)
	 
	  val sc = new SparkContext(conf)
	  val cc = new CassandraSQLContext(sc)
	
		//Read values from cassandra and rename label column not to conflict with
		//Random forest label column
	  val originalDf = getDataset("train", cc, keySpace,inputColumnFamily)
	  val testOriginalDf = getDataset("test", cc, keySpace,inputColumnFamily)
	    //create Random Forest Model
	  val rf = new RandomForestClassifier()
		  .setNumTrees(numTrees)
      .setFeatureSubsetStrategy("auto")
      .setImpurity("gini")
      .setLabelCol("indexLabel")
    //Use the transform required by RandomForest
	  val indexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexLabel")

    //create the pipeline
	  val pipeline = new Pipeline().setStages(Array(indexer,rf))
	  
	  //remove the columns that should not be used as features
	  val  featuresDf : DataFrame = dropBadColumns(originalDf)
	  val testFeaturesDf : DataFrame = dropBadColumns(testOriginalDf)

	  //Create assembler to create column features as required by the model
	  val assembler = new VectorAssembler()
	    .setInputCols((featuresDf.columns.clone().drop(featuresDf.columns.indexOf("label")) ++ 
	        featuresDf.columns.clone().dropRight(featuresDf.columns.indexOf("label"))).distinct )
	    .setOutputCol("features")
	  
	  //assert assembler didnt break the dataframe  
	  assert(assembler.getInputCols.length == featuresDf.columns.length)
	  
	  //convert strings to double
	  val convertedDf = castColumnsToDouble(featuresDf, featuresDf.columns.length)
	  val convertedTestDf = castColumnsToDouble(testFeaturesDf, testFeaturesDf.columns.length)
	  
	  //assemble columns on required format
	  val trainingDf = assembler.transform(convertedDf)
	  val testDf = assembler.transform(convertedTestDf)

	
//	  val model = pipeline.fit(trainingDf) 
	  val evaluator = new MulticlassClassificationEvaluator()
  .setLabelCol("label")
  .setPredictionCol("prediction")
  // "f1", "precision", "recall", "weightedPrecision", "weightedRecall"
  .setMetricName("precision") 
  	 val paramGrid = new ParamGridBuilder().build()
//Create cross validation pipeline
  val cv = new CrossValidator()
  .setEstimator(pipeline)
  .setEvaluator(evaluator) 
  .setEstimatorParamMaps(paramGrid)
  .setNumFolds(numFolds)
  
  
  
  
  //train model and get results
	  val cvModel = cv.fit(trainingDf)
    val trainResults = cvModel.transform(trainingDf).select("label", "prediction")
    trainResults.show()
    val testResults = cvModel.transform(testDf)
    //persist training error
    persistError("scala_train_"+ evaluator.hashCode().toString(),"train",numTrees,evaluator.evaluate((
        trainResults)),conf, keySpace,outputColumnFamily)
    //persist test error
    persistError("scala_test_"+ evaluator.hashCode().toString(),"test",numTrees,evaluator.evaluate((
        testResults)),conf, keySpace,outputColumnFamily)
        
     //persist model to hdfs
     sc.parallelize(Seq(cvModel), 1).saveAsObjectFile("hdfs://localhost:9000/user/root/randomforest"
         +evaluator.hashCode().toString()+".model")




	// terminate spark context
	sc.stop()

	}
}