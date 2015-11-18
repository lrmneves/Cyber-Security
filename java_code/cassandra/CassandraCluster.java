package cassandra;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import randomforestfiles.RandomForestUtils;


public class CassandraCluster
{
	private static Cluster cluster = null;
	private final static String keyspace = "bigdata_class";;
	private static Session session = null;
	private static Host host = null;

	public static void connect(final String node, final int port)
	{  if(cluster != null) return;
	cluster = Cluster.builder().addContactPoint(node).withPort(port).build();
	final Metadata metadata = cluster.getMetadata();
	out.printf("Connected to cluster: %s\n", metadata.getClusterName());
	for (final Host host : metadata.getAllHosts())
	{	

		out.printf("Datacenter: %s; Host: %s; Rack: %s\n",
				host.getDatacenter(), host.getAddress(), host.getRack());
		CassandraCluster.host = host;
	}
	session = cluster.connect();
	}

	public static void connect(){
		connect("localhost",9042); 
	}

	public static Session getSession()
	{
		return session;
	}
	public static List<Row> getHeaders(){
		Statement statement = QueryBuilder
				.select()
				.all()
				.from(keyspace, "header_order");

		return getSession()
				.execute(statement)
				.all();
	}
	public static List<Row> selectAll() {
		return selectAll("train");
	} 
	public static List<Row> selectAll(String name) {
		Statement statement = QueryBuilder
				.select()
				.all()
				.from(keyspace,"cyber_sec")
				.where(QueryBuilder.eq("type",name));


		return getSession()
				.execute(statement)
				.all();
	} 
	public static void persistErrorValues(String exp, String type, int treeNum, double error){
		session.execute("USE "+keyspace);

		String query = "INSERT INTO result (experiment,type, tree_num, error) VALUES('"
				+ exp+"','"+type+"'," + treeNum + ',' + error+");";
		try{
			session.execute(query);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void createKeyspace(){
		String query = "DROP KEYSPACE " + keyspace+ " ; ";
		try{
			session.execute(query);
		}catch(Exception e){}
		query = "CREATE KEYSPACE " + keyspace+ " WITH replication "
				+ "= {'class':'SimpleStrategy', 'replication_factor':1}; ";

		try{
			session.execute(query);
		}catch(Exception e){}

		session.execute("USE "+keyspace);
	}
	//Creates table to keep headers order and initialize the the id header
	public static void createHeadersTable(){
		String query = "CREATE TABLE header_order (ord int, name text,PRIMARY KEY "
				+ "((name),ord)) WITH CLUSTERING ORDER BY (ord ASC);";
		try{
			session.execute(query);
		}catch(Exception e){
			e.printStackTrace();
		}
		query = "INSERT INTO header_order (ord,name) VALUES(0,'id')";
		try{
			session.execute(query);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void createResultsTable(){
		String query = "CREATE TABLE result (experiment text,type text, tree_num int, error double, PRIMARY KEY "
				+ "((experiment,type),tree_num)) WITH CLUSTERING ORDER BY (tree_num ASC);";
		try{
			session.execute(query);
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	public static void createTreeTable(){
		String query = "CREATE TABLE tree_table (experiment text PRIMARY KEY,tree text) with COMPACT STORAGE ;";
		try{
			session.execute(query);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void resetTreeTable(){
		String query = "DROP TABLE tree_table;";
		try{
			session.execute(query);
		}catch(Exception e){
			e.printStackTrace();
		}
		createTreeTable();
	}
	public static void createForestTable(){
		String query = "CREATE TABLE forest_table (experiment text PRIMARY KEY,forest text) with COMPACT STORAGE ;";
		try{
			session.execute(query);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static String getForestString(String experiment){
		Statement statement = QueryBuilder
				.select()
				.all()
				.from(keyspace,"forest_table")
				.where(QueryBuilder.eq("experiment",experiment));
		List<Row> result = getSession()
				.execute(statement)
				.all();
		if(result.size() == 0) return null;
		Row r = result.get(0);
		
		return r.getString("forest");
	}
	public static void  createTable(String trainFile,String testFile) throws IOException{



		BufferedReader br = null;
		String line = "";
		br = new BufferedReader(new FileReader(trainFile));
		String[] headers = br.readLine().split(RandomForestUtils.CSV_SPLIT_BY);
		String[] path = trainFile.split("/");
		String pathend = path[path.length-1];


		createKeyspace();

		createHeadersTable();
		createTreeTable();
		createForestTable();
		//Creates cyber_sec table with the columns from csv and the prefix of the insert statement
		StringBuilder builder = new StringBuilder("CREATE TABLE cyber_sec (id text "
				+ "PRIMARY KEY, ");
		StringBuilder inserter = new StringBuilder("INSERT INTO cyber_sec (id,");
		for(int i = 1; i < headers.length-1;i++){
			builder.append(headers[i]+" text, ");
			inserter.append(headers[i]).append(", ");
			try{
				//store the header order on a table
				session.execute("INSERT INTO header_order (ord,name) VALUES("+ i +", '"+headers[i] +"');");

			}catch(Exception e){
				e.printStackTrace();
			}
		}

		builder.append(headers[headers.length-1]).append(" text, type text );");
		session.execute("INSERT INTO header_order (ord,name) VALUES("+ (headers.length-1) +", '"+headers[headers.length-1] +"');");
		//finalizes the prefix for inserting elements
		inserter.append(headers[headers.length-1]).append(", type) VALUES(");
		//creates table
		try{
			session.execute(builder.toString());
		}catch(Exception e){
			e.printStackTrace();
		}
		//creates index so we can search for the type:train, test
		String index = "CREATE INDEX ON "+ keyspace +".cyber_sec (type);";
		try{
			session.execute(index);
		}catch(Exception e){
			e.printStackTrace();
		}
		//Insert values from csv into cassandra table
		while ((line = br.readLine()) != null) {
			builder = new StringBuilder();
			String [] values = line.split(RandomForestUtils.CSV_SPLIT_BY);

			for(int i = 0; i < values.length-1;i++){
				builder.append("'" + values[i]+"'").append(",");
			}
			builder.append("'" + values[values.length-1]+"'" ).append(", 'train');");
			try{
				session.execute(inserter.toString()+ builder.toString());
			}catch(Exception e){
				//					e.printStackTrace();
			}
		}
		br = new BufferedReader(new FileReader(testFile));
		br.readLine();
		while ((line = br.readLine()) != null) {
			builder = new StringBuilder();
			String [] values = line.split(RandomForestUtils.CSV_SPLIT_BY);

			for(int i = 0; i < values.length-1;i++){
				builder.append("'" + values[i]+"'").append(",");
			}
			builder.append("'" + values[values.length-1]+"'" ).append(", 'test');");
			try{
				session.execute(inserter.toString()+ builder.toString());
			}catch(Exception e){
				//					e.printStackTrace();
			}
		}
		//create results table
		createResultsTable();
	}
	public static String getAddress(){
		return host.getAddress().toString();
	}
	public static void close()
	{
		cluster.close();
		cluster = null;
	}
	public static void startKeyspace() {
		if(keyspace == null){
			session.execute("USE bigdata_class;");

			return;
		}
		session.execute("USE "+keyspace);

	}
	public static String getKeyspace() {
		// TODO Auto-generated method stub
		return keyspace;
	}
}