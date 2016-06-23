package org.aksw.simba.dataset.lsq;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

import org.aksw.simba.benchmark.Config;
import org.aksw.simba.benchmark.encryption.EncryptUtils;
import org.aksw.simba.benchmark.log.operations.DBpediaLogReader;
import org.aksw.simba.benchmark.log.operations.DateConverter.DateParseException;
import org.aksw.simba.benchmark.log.operations.LinkedGeoDataLogReader;
import org.aksw.simba.benchmark.log.operations.SesameLogReader;
import org.aksw.simba.benchmark.spin.Spin;
import org.aksw.simba.largerdfbench.util.QueryStatistics;
import org.aksw.simba.largerdfbench.util.Selectivity;
import org.aksw.simba.benchmark.log.operations.*;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
/**
 * This is the main class used to RDFise query logs 
 * @author Saleem
 *
 */
public class LogRDFizer {
	public static BufferedWriter 	bw ;
	public  RepositoryConnection con = null;
	public static  BufferedWriter tobw= null;
	public static long queryNo = 1;
	public  static int  maxRunTime ;  //max query execution time in seconds
	public int runtimeErrorCount;
	public long endpointSize = 0;
	public static void main(String[] args) throws IOException, RepositoryException, MalformedQueryException, QueryEvaluationException, ParseException, DateParseException {
		//String queryLogDir = "D:/QueryLogs/SWDF-Test/";  //dont forget last /
		 // String queryLogDir = "/home/MuhammadSaleem/dbpedia351logs/";
		  String queryLogDir = Config.queryLogDir;
		// String queryLogDir = "D:/QueryLogs/RKBExplorer/";
				 
		 String acronym = "DBP" ; //  a short acronym of the dataset
		
		 String localEndpoint = Config.endpoint;
		// String localEndpoint = "http://linkedgeodata.org/sparql";
		
		//String graph = "http://aksw.org/benchmark"; //Named graph. can be null
		String graph = Config.graph; //can be null
		//String graph = null;
		
		String outputFile = "Queries.ttl";
		//String outputFile = "LinkedDBpedia351SQL.ttl";
		//String outputFile = "Linked-SQ-DBpedia-Fixed.ttl";
		
		// String publicEndpoint = "http://data.semanticweb.org/sparql";
		//String publicEndpoint = "http://dbpedia.org/sparql";
		String publicEndpoint = Config.endpoint;
		
		maxRunTime = Config.max_run_time;  //Max query runtime in seconds
		tobw = new BufferedWriter(new FileWriter("timeOutQueries.txt")); // the location where time out queries will be stored
		String separator = "- -";   // this is separator which separates the agent ip (encrypted) and corresponding exe time. can be null if there is no user I.P provided in log
		//String separator = null;  //null is when IP is missing. like in BM
		
		//SesameLogReader slr = new SesameLogReader();
		 DBpediaLogReader dblr = new DBpediaLogReader();
		//RKBExplorerLogReader rkblr = new RKBExplorerLogReader();
		//LinkedGeoDataLogReader lglr = new LinkedGeoDataLogReader();  //here is your parser class
		
		LogRDFizer rdfizer = new LogRDFizer();
		
		//Map<String, Set<String>> queryToSubmissions = slr.getSesameQueryExecutions(queryLogDir);  // this map contains a query as key and their all submissions. 
		//Note submission is combination  of  three strings: hashed I.P + separator String+ Exectuion time in xsd:dateTimeFormat. Note the the dateTime format is strict. 
		Map<String, Set<String>> queryToSubmissions = dblr.getVirtuosoQueryExecutions(queryLogDir);  // this map contains a query as key and their all submissions
	//	Map<String, Set<String>> queryToSubmissions = rkblr.getBritishMuseumQueryExecutions(queryLogDir); 
	//	Map<String, Set<String>> queryToSubmissions = lglr.getVirtuosoQueryExecutions(queryLogDir);
		
		System.out.println(queryToSubmissions.keySet().size());
		System.out.println("Number of Distinct queries: " +  queryToSubmissions.keySet().size());
		//-----Once you get the query to submissions map by parsing the log then the below method is general---
		rdfizer.rdfizeLog(queryToSubmissions,localEndpoint,publicEndpoint,graph,outputFile,separator,acronym);
		System.out.println("Dataset stored at " + outputFile);
	}
	
	
	/**
	 * add some prefixes and remove some virtuoso specific syntactic sugar
	 * 
	 * @param queryStr the query to clean
	 * @return cleaned query
	 */
	public static String rewriteQuery(String queryStr) 
	{
		String prefixes = "Prefix bif: <bif:>\nPrefix dbprop: <http://dbpedia.org/property/>\nPrefix dcterms: <http://purl.org/dc/terms/>\nPrefix a: <http://www.w3.org/2005/Atom>\nPrefix address: <http://schemas.talis.com/2005/address/schema#>\nPrefix admin: <http://webns.net/mvcb/>\nPrefix atom: <http://atomowl.org/ontologies/atomrdf#>\nPrefix aws: <http://soap.amazon.com/>\nPrefix b3s: <http://b3s.openlinksw.com/>\nPrefix batch: <http://schemas.google.com/gdata/batch>\nPrefix bibo: <http://purl.org/ontology/bibo/>\nPrefix bugzilla: <http://www.openlinksw.com/schemas/bugzilla#>\nPrefix c: <http://www.w3.org/2002/12/cal/icaltzd#>\nPrefix campsite: <http://www.openlinksw.com/campsites/schema#>\nPrefix cb: <http://www.crunchbase.com/>\nPrefix cc: <http://web.resource.org/cc/>\nPrefix content: <http://purl.org/rss/1.0/modules/content/>\nPrefix cv: <http://purl.org/captsolo/resume-rdf/0.2/cv#>\nPrefix cvbase: <http://purl.org/captsolo/resume-rdf/0.2/base#>\nPrefix dawgt: <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#>\nPrefix dbc: <http://dbpedia.org/resource/Category:>\nPrefix dbo: <http://dbpedia.org/ontology/>\nPrefix dbp: <http://dbpedia.org/property/>\nPrefix dbr: <http://dbpedia.org/resource/>\nPrefix dc: <http://purl.org/dc/elements/1.1/>\nPrefix dct: <http://purl.org/dc/terms/>\nPrefix digg: <http://digg.com/docs/diggrss/>\nPrefix dul: <http://www.ontologydesignpatterns.org/ont/dul/DUL.owl>\nPrefix ebay: <urn:ebay:apis:eBLBaseComponents>\nPrefix enc: <http://purl.oclc.org/net/rss_2.0/enc#>\nPrefix exif: <http://www.w3.org/2003/12/exif/ns/>\nPrefix fb: <http://api.facebook.com/1.0/>\nPrefix ff: <http://api.friendfeed.com/2008/03>\nPrefix fn: <http://www.w3.org/2005/xpath-functions/#>\nPrefix foaf: <http://xmlns.com/foaf/0.1/>\nPrefix freebase: <http://rdf.freebase.com/ns/>\nPrefix g: <http://base.google.com/ns/1.0>\nPrefix gb: <http://www.openlinksw.com/schemas/google-base#>\nPrefix gd: <http://schemas.google.com/g/2005>\nPrefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\nPrefix geonames: <http://www.geonames.org/ontology#>\nPrefix georss: <http://www.georss.org/georss/>\nPrefix gml: <http://www.opengis.net/gml>\nPrefix go: <http://purl.org/obo/owl/GO#>\nPrefix hlisting: <http://www.openlinksw.com/schemas/hlisting/>\nPrefix hoovers: <http://wwww.hoovers.com/>\nPrefix ical: <http://www.w3.org/2002/12/cal/ical#>\nPrefix ir: <http://web-semantics.org/ns/image-regions>\nPrefix itunes: <http://www.itunes.com/DTDs/Podcast-1.0.dtd>\nPrefix ldp: <http://www.w3.org/ns/ldp#>\nPrefix lgv: <http://linkedgeodata.org/vocabulary#>\nPrefix link: <http://www.xbrl.org/2003/linkbase>\nPrefix lod: <http://lod.openlinksw.com/>\nPrefix math: <http://www.w3.org/2000/10/swap/math#>\nPrefix media: <http://search.yahoo.com/mrss/>\nPrefix mesh: <http://purl.org/commons/record/mesh/>\nPrefix meta: <urn:oasis:names:tc:opendocument:xmlns:meta:1.0>\nPrefix mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>\nPrefix mmd: <http://musicbrainz.org/ns/mmd-1.0#>\nPrefix mo: <http://purl.org/ontology/mo/>\nPrefix mql: <http://www.freebase.com/>\nPrefix nci: <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\nPrefix nfo: <http://www.semanticdesktop.org/ontologies/nfo/#>\nPrefix ng: <http://www.openlinksw.com/schemas/ning#>\nPrefix nyt: <http://www.nytimes.com/>\nPrefix oai: <http://www.openarchives.org/OAI/2.0/>\nPrefix oai_dc: <http://www.openarchives.org/OAI/2.0/oai_dc/>\nPrefix obo: <http://www.geneontology.org/formats/oboInOwl#>\nPrefix office: <urn:oasis:names:tc:opendocument:xmlns:office:1.0>\nPrefix ogc: <http://www.opengis.net/>\nPrefix ogcgml: <http://www.opengis.net/ont/gml#>\nPrefix ogcgs: <http://www.opengis.net/ont/geosparql#>\nPrefix ogcgsf: <http://www.opengis.net/def/function/geosparql/>\nPrefix ogcgsr: <http://www.opengis.net/def/rule/geosparql/>\nPrefix ogcsf: <http://www.opengis.net/ont/sf#>\nPrefix oo: <urn:oasis:names:tc:opendocument:xmlns:meta:1.0:>\nPrefix openSearch: <http://a9.com/-/spec/opensearchrss/1.0/>\nPrefix opencyc: <http://sw.opencyc.org/2008/06/10/concept/>\nPrefix opl: <http://www.openlinksw.com/schema/attribution#>\nPrefix opl-gs: <http://www.openlinksw.com/schemas/getsatisfaction/>\nPrefix opl-meetup: <http://www.openlinksw.com/schemas/meetup/>\nPrefix opl-xbrl: <http://www.openlinksw.com/schemas/xbrl/>\nPrefix oplweb: <http://www.openlinksw.com/schemas/oplweb#>\nPrefix ore: <http://www.openarchives.org/ore/terms/>\nPrefix owl: <http://www.w3.org/2002/07/owl#>\nPrefix product: <http://www.buy.com/rss/module/productV2/>\nPrefix protseq: <http://purl.org/science/protein/bysequence/>\nPrefix r: <http://backend.userland.com/rss2>\nPrefix radio: <http://www.radiopop.co.uk/>\nPrefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPrefix rdfa: <http://www.w3.org/ns/rdfa#>\nPrefix rdfdf: <http://www.openlinksw.com/virtrdf-data-formats#>\nPrefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\nPrefix rss: <http://purl.org/rss/1.0/>\nPrefix sc: <http://purl.org/science/owl/sciencecommons/>\nPrefix scovo: <http://purl.org/NET/scovo#>\nPrefix sd: <http://www.w3.org/ns/sparql-service-description#>\nPrefix sf: <urn:sobject.enterprise.soap.sforce.com>\nPrefix sioc: <http://rdfs.org/sioc/ns#>\nPrefix sioct: <http://rdfs.org/sioc/types#>\nPrefix skiresort: <http://www.openlinksw.com/ski_resorts/schema#>\nPrefix skos: <http://www.w3.org/2004/02/skos/core#>\nPrefix slash: <http://purl.org/rss/1.0/modules/slash/>\nPrefix sql: <sql:>\nPrefix stock: <http://xbrlontology.com/ontology/finance/stock_market#>\nPrefix twfy: <http://www.openlinksw.com/schemas/twfy#>\nPrefix umbel: <http://umbel.org/umbel#>\nPrefix umbel-ac: <http://umbel.org/umbel/ac/>\nPrefix umbel-rc: <http://umbel.org/umbel/rc/>\nPrefix umbel-sc: <http://umbel.org/umbel/sc/>\nPrefix uniprot: <http://purl.uniprot.org/>\nPrefix units: <http://dbpedia.org/units/>\nPrefix usc: <http://www.rdfabout.com/rdf/schema/uscensus/details/100pct/>\nPrefix v: <http://www.openlinksw.com/xsltext/>\nPrefix vcard: <http://www.w3.org/2001/vcard-rdf/3.0#>\nPrefix vcard2006: <http://www.w3.org/2006/vcard/ns#>\nPrefix vi: <http://www.openlinksw.com/virtuoso/xslt/>\nPrefix virt: <http://www.openlinksw.com/virtuoso/xslt>\nPrefix virtcxml: <http://www.openlinksw.com/schemas/virtcxml#>\nPrefix virtpivot: <http://www.openlinksw.com/schemas/virtpivot#>\nPrefix virtrdf: <http://www.openlinksw.com/schemas/virtrdf#>\nPrefix void: <http://rdfs.org/ns/void#>\nPrefix wb: <http://www.worldbank.org/>\nPrefix wdrs: <http://www.w3.org/2007/05/powder-s#>\nPrefix wf: <http://www.w3.org/2005/01/wf/flow#>\nPrefix wfw: <http://wellformedweb.org/CommentAPI/>\nPrefix wikicompany: <http://dbpedia.openlinksw.com/wikicompany/>\nPrefix wikidata: <http://www.wikidata.org/entity/>\nPrefix xf: <http://www.w3.org/2004/07/xpath-functions>\nPrefix xfn: <http://gmpg.org/xfn/11#>\nPrefix xhtml: <http://www.w3.org/1999/xhtml>\nPrefix xhv: <http://www.w3.org/1999/xhtml/vocab#>\nPrefix xi: <http://www.xbrl.org/2003/instance>\nPrefix xml: <http://www.w3.org/XML/1998/namespace>\nPrefix xn: <http://www.ning.com/atom/1.0>\nPrefix xsd: <http://www.w3.org/2001/XMLSchema#>\nPrefix xsl10: <http://www.w3.org/XSL/Transform/1.0>\nPrefix xsl1999: <http://www.w3.org/1999/XSL/Transform>\nPrefix xslwd: <http://www.w3.org/TR/WD-xsl>\nPrefix y: <urn:yahoo:maps>\nPrefix yago: <http://dbpedia.org/class/yago/>\nPrefix yago-res: <http://mpii.de/yago/resource/>\nPrefix yt: <http://gdata.youtube.com/schemas/2007>\nPrefix zem: <http://s.zemanta.com/ns#>\n";
		queryStr = queryStr.replaceAll("define sql:describe-mode '[^\\']*'", ""); // remove virtuoso query sugar
		queryStr = queryStr.replaceAll("define sql:describe-mode \"[^\"]*\"", "");
		return prefixes+queryStr;
	}
	
	/**
	 * RDFize Log	
	 * @param queryToSubmissions A map which store a query string (single line) as key and all the corresponding submissions as List. Where a submission is a combination
	 * of User encrypted ip and the data,time of the query request. The I.P and the time is separated by a separator
	 * @param localEndpoint Endpoint which will be used for feature generation
	 * @param publicEndpoint Public endpoint of the log
	 * @param graph named Graph, can be null
	 * @param outputFile The output RDF file
	 * @param separator Submission separator. Explained above
	 * @param acronym A Short acronym of the dataset log, e.g., DBpedia or SWDF
	 * @throws IOException
	 * @throws RepositoryExceptiond
	 * @throws ParseException 
	 */
	public void rdfizeLog(Map<String, Set<String>> queryToSubmissions, String localEndpoint, String publicEndpoint, String graph, String outputFile, String separator, String acronym) throws IOException, RepositoryException, MalformedQueryException, QueryEvaluationException, ParseException {
		System.out.println("RDFization started...");
		endpointSize = Selectivity.getEndpointTotalTriples(localEndpoint, graph);
		long parseErrorCount =0;
		bw = new BufferedWriter(new FileWriter(outputFile));
		this.writePrefixes(acronym);
	  for(String queryStr: queryToSubmissions.keySet())
		{
		    System.out.print("\r"+queryNo+" Started... -- "+((queryNo/(queryToSubmissions.size()*1.0))*100.0)+" % ");
			//bw.write("\nlsqv:LinkedSQL  lsqv:hasLogOf    lsqrd:q-"+queryNo+ " . \n");
			bw.write("lsqrd:q"+queryNo+ " lsqv:endpoint <" + publicEndpoint + "> ; \n");
			String ttlqueryStr = queryStr.replace("\\", "\\\\"); //escape query to embed in turtle literal
			ttlqueryStr = ttlqueryStr.replace("\"", "\\\""); //escape query to embed in turtle literal
			
			bw.write(" sp:text \""+ttlqueryStr+"\" ; \n"); 
			queryNo++;
			Query query =  new Query();
			try{
				/*query modifications to get not too much parse errors*/
				String querySub = rewriteQuery(queryStr);
				
				String querySubttl = querySub.replace("\\", "\\\\"); //escape query to embed in turtle literal
				querySubttl = querySubttl.replace("\"", "\\\""); //escape query to embed in turtle literal
				bw.write(" lsqv:rewrittenQuery \""+querySubttl+"\" ; \n");
				
				query = QueryFactory.create(querySub);

			}
			catch (Exception ex){
				String parseError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
				bw.write(" lsqv:parseError \""+parseError+ "\" . ");
				String queryStats = this.getRDFUserExecutions(queryToSubmissions.get(queryStr),separator);
				bw.write(queryStats);
				parseErrorCount++;}
			try{
				if(query.isDescribeType())
					this.RDFizeDescribe(query,queryStr,localEndpoint,graph,queryToSubmissions.get(queryStr),separator);
				else if (query.isSelectType())
					this.RDFizeSelect(query,queryStr,localEndpoint,graph,queryToSubmissions.get(queryStr),separator);
				else if (query.isAskType())
					this.RDFizeASK(query,queryStr,localEndpoint,graph,queryToSubmissions.get(queryStr),separator);
				else if (query.isConstructType())
					this.RDFizeConstruct(query,queryStr,localEndpoint,graph,queryToSubmissions.get(queryStr),separator);
			}
			catch(Exception ex){}
		}
		bw.close();
		System.out.println("Total Number of Queries with Parse Errors: " + parseErrorCount);	
		System.out.println("Total Number of Queries with Runtime Errors: " + runtimeErrorCount);	
	}

	/**
	 * RDFized SELECT query
	 * @param query Query
	 * @param localEndpoint Local endpoint
	 * @param graph Named Graph, can be null
	 * @param submissions List of all submissions (I.P:ExecutionTime) of the given query
	 * @param separator Separator string between I.P and execution time
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws ParseException 
	 * @throws QueryEvaluationException 
	 */
	public void RDFizeSelect(Query query,String orig_query, String localEndpoint, String graph, Set<String> submissions, String separator) throws IOException, RepositoryException, MalformedQueryException, ParseException, QueryEvaluationException {
		String queryStats ="";
		long curTime = System.currentTimeMillis();
		try {
			Query queryNew = SesameLogReader.removeNamedGraphs(query);
			long resultSize = this.getQueryResultSize(queryNew.toString(),orig_query, localEndpoint,"select");
			long exeTime = System.currentTimeMillis() - curTime ;
			//queryStats =queryStats+" lsqv:queryType \"SELECT\" ; " ;
			//queryStats =queryStats+" lsqv:hasFeatures lsqrd:f-q"+(queryNo-1)+" . \n " ;
			//queryStats =queryStats+" lsqv:hasClauses clause:q"+(queryNo-1)+" . \n" ;
			//queryStats = queryStats +"lsqrd:f-q"+(queryNo-1);
			queryStats = queryStats + " lsqv:resultSize "+resultSize  +" ; ";
			queryStats = queryStats+" lsqv:runTimeMs "+exeTime  +" ; ";
			queryStats = queryStats+QueryStatistics.getRDFizedQueryStats(query,localEndpoint,graph,endpointSize, queryStats);
			bw.write(queryStats);

		} catch (Exception ex) {String runtimeError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
		bw.write(" lsqv:runtimeError \""+runtimeError+ "\" . ");
		runtimeErrorCount++; }
		queryStats = QueryStatistics.rdfizeTuples_JoinVertices(query.toString());
		bw.write(queryStats);
		queryStats = this.getRDFUserExecutions(submissions,separator);
		bw.write(queryStats);
		queryStats = this.getSpinRDFStats(query);
		bw.write(queryStats);
	}
	/**
	 * RDFized DESCRIBE query
	 * @param query Query
	 * @param localEndpoint Local endpoint
	 * @param graph Named Graph, can be null
	 * @param submissions List of all submissions (I.P:ExecutionTime) of the given query
	 * @param separator Separator string between I.P and execution time
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws ParseException 
	 * @throws QueryEvaluationException 
	 */
	public void RDFizeDescribe(Query query, String orig_query,String localEndpoint, String graph, Set<String> submissions, String separator) throws IOException, RepositoryException, MalformedQueryException, ParseException, QueryEvaluationException {
		String queryStats ="";
		long curTime = System.currentTimeMillis();
		try {
			Query queryNew = SesameLogReader.removeNamedGraphs(query);
			long resultSize = this.getQueryResultSize(queryNew.toString(),orig_query, localEndpoint,"describe");
			long exeTime = System.currentTimeMillis() - curTime ;
			//queryStats =queryStats+" lsqv:queryType \"DESCRIBE\" ; " ;
			//queryStats =queryStats+" lsqv:hasFeatures lsqrd:f-q"+(queryNo-1)+" . \n" ;
			//queryStats =queryStats+" lsqv:hasClauses clause:q"+(queryNo-1)+" . \n" ;
			//queryStats = queryStats +"lsqrd:f-q"+(queryNo-1);
			queryStats = queryStats + " lsqv:resultSize "+resultSize  +" ; ";
			queryStats = queryStats+" lsqv:runTimeMs "+exeTime  +" ; ";
			queryStats = queryStats+QueryStatistics.getRDFizedQueryStats(query,localEndpoint,graph,endpointSize, queryStats);
			bw.write(queryStats);

		} catch (Exception ex) {String runtimeError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
		bw.write(" lsqv:runtimeError \""+runtimeError+ "\" . ");
		runtimeErrorCount++; }
		queryStats = QueryStatistics.rdfizeTuples_JoinVertices(query.toString());
		bw.write(queryStats);
		queryStats = this.getRDFUserExecutions(submissions,separator);
		bw.write(queryStats);
		queryStats = this.getSpinRDFStats(query);
		bw.write(queryStats);
	}
	/**
	 * RDFized CONSTRUCT query
	 * @param query Query
	 * @param localEndpoint Local endpoint
	 * @param graph Named Graph, can be null
	 * @param submissions List of all submissions (I.P:ExecutionTime) of the given query
	 * @param separator Separator string between I.P and execution time
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws ParseException 
	 * @throws QueryEvaluationException 
	 */
	public void RDFizeConstruct(Query query, String orig_query,String localEndpoint, String graph, Set<String> submissions, String separator) throws IOException, RepositoryException, MalformedQueryException, ParseException, QueryEvaluationException {
		String queryStats ="";
		long curTime = System.currentTimeMillis();
		try {
			Query queryNew = SesameLogReader.removeNamedGraphs(query);
			long resultSize = this.getQueryResultSize(queryNew.toString(),orig_query, localEndpoint,"construct");
			long exeTime = System.currentTimeMillis() - curTime ;
			//queryStats =queryStats+" lsqv:queryType \"CONSTRUCT\" ; " ;
			//queryStats =queryStats+" lsqv:hasFeatures lsqrd:f-q"+(queryNo-1)+" . \n " ;
			//queryStats =queryStats+" lsqv:hasClauses clause:q"+(queryNo-1)+" . \n" ;
			//queryStats = queryStats +"lsqrd:f-q"+(queryNo-1);
			queryStats = queryStats + " lsqv:resultSize "+resultSize  +" ; ";
			queryStats = queryStats+" lsqv:runTimeMs "+exeTime  +" ; ";
			queryStats = queryStats+QueryStatistics.getRDFizedQueryStats(query,localEndpoint,graph,endpointSize, queryStats);
			bw.write(queryStats);

		} catch (Exception ex) {String runtimeError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
		bw.write(" lsqv:runtimeError \""+runtimeError+ "\" . ");
		runtimeErrorCount++; }
		queryStats = QueryStatistics.rdfizeTuples_JoinVertices(query.toString());
		bw.write(queryStats);
		queryStats = this.getRDFUserExecutions(submissions,separator);
		bw.write(queryStats);
		queryStats = this.getSpinRDFStats(query);
		bw.write(queryStats);
	}
	/**
	 * RDFized ASK query
	 * @param query Query
	 * @param localEndpoint Local endpoint
	 * @param graph Named Graph, can be null
	 * @param submissions List of all submissions (I.P:ExecutionTime) of the given query
	 * @param separator Separator string between I.P and execution time
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws ParseException 
	 * @throws QueryEvaluationException 
	 */
	public void RDFizeASK(Query query, String orig_query,String localEndpoint, String graph, Set<String> submissions, String separator) throws IOException, RepositoryException, MalformedQueryException, ParseException, QueryEvaluationException {
		String queryStats ="";
		long curTime = System.currentTimeMillis();
		try {
			Query queryNew = SesameLogReader.removeNamedGraphs(query);
			long resultSize = this.getQueryResultSize(queryNew.toString(),orig_query, localEndpoint,"ask");
			long exeTime = System.currentTimeMillis() - curTime ;
			//queryStats =queryStats+" lsqv:queryType \"ASK\" ; " ;
			//queryStats =queryStats+" lsqv:hasFeatures lsqrd:f-q"+(queryNo-1)+" . \n " ;
			//queryStats =queryStats+" lsqv:hasClauses clause:q"+(queryNo-1)+" . \n" ;
			//queryStats = queryStats +"lsqrd:f-q"+(queryNo-1);
			queryStats = queryStats + " lsqv:resultSize "+resultSize  +" ; ";
			queryStats = queryStats+" lsqv:runTimeMs "+exeTime  +" ; ";
			queryStats = queryStats+QueryStatistics.getRDFizedQueryStats(query,localEndpoint,graph,endpointSize, queryStats);
			bw.write(queryStats);

		} catch (Exception ex) {String runtimeError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
		bw.write(" lsqv:runtimeError \""+runtimeError+ "\" . ");
		runtimeErrorCount++; }
		queryStats = QueryStatistics.rdfizeTuples_JoinVertices(query.toString());
		bw.write(queryStats);
		queryStats = this.getQueryTuples(query);
		queryStats = this.getRDFUserExecutions(submissions,separator);
		bw.write(queryStats);
		queryStats = this.getSpinRDFStats(query);
		bw.write(queryStats);
	}
	public String getQueryTuples(Query query) {
		
		return null;
	}
	/**
	 * Get all executions (IP,Time) of the given query
	 * @param query Query
	 * @param submissions  Query submissions in form of IP:Time
	 * @param separator String separator between IP:Time 
	 * @return Stats
	 * @throws ParseException 
	 */
	public String getRDFUserExecutions(Set<String> submissions, String separator) throws ParseException {
		String queryStats = "\nlsqrd:q"+(LogRDFizer.queryNo-1);
		queryStats = queryStats+ " lsqv:execution ";
		int subCount = 1;
		for(int i=0; i<submissions.size();i++)
		{
			if(i<submissions.size()-1)
			{
				queryStats = queryStats + "lsqrd:q"+(queryNo-1)+"-e"+subCount+ " , ";
			}
			else
			{
				queryStats = queryStats + "lsqrd:q"+(queryNo-1)+"-e"+subCount+ " . \n ";
			}
			subCount++;
		}
		int j = 1;
		if(!(separator==null))  //i.e both I.P and Time is provided in log
		{
		for(String submission:submissions)
		{
			String prts [] = submission.split(separator);
			String txt=prts[0].replace(".", "useanystring") ; // of course we used different one
			String key="what is your key string?";  //of course we use different key in LSQ. 
			txt=EncryptUtils.xorMessage( txt, key );
			String encoded=EncryptUtils.base64encode( txt ); 
			encoded = encoded.replace("=", "-");
			encoded = encoded.replace("+", "-");
		   	queryStats = queryStats + "lsqrd:q"+(queryNo-1)+"-e"+j+ " lsqv:agent lsqr:A-"+prts[0]+"  ; dct:issued \""+prts[1]+"\"^^xsd:dateTimeStamp . \n";
			j++;
		}
		}
		else  //only exe time is stored
		{
			for(String submission:submissions)
			{
				queryStats = queryStats + "lsqrd:q"+(queryNo-1)+"-e"+j+ " dct:issued \""+submission+"\"^^xsd:dateTimeStamp . \n";
				j++;
			}
			
		}
		return queryStats;

	}

	/**
	 * Get Spin RDF stats of the qiven query
	 * @param query Query
	 * @return Spin Stas
	 * @throws IOException
	 */
	private String getSpinRDFStats(Query query) throws IOException {
		String queryStats = "";
		try {
			Spin sp = new Spin();
			String spinQuery = sp.getSpinRDF(query.toString(), "Turtle");
			String prefix = spinQuery.substring(0,spinQuery.indexOf("[")-1);
			String body = spinQuery.substring(spinQuery.indexOf("[")+1,spinQuery.lastIndexOf("]"));
			spinQuery = prefix+" lsqrd:q"+(queryNo-1)+"  "+ body;
			//queryStats = queryStats+ "lsqrd:q"+(LogRDFizer.queryNo-1);
			//queryStats = queryStats+" lsqv:spinQuery lsqrd:q"+(queryNo-1)  +" . \n";
			queryStats = queryStats+spinQuery +" . \n";	

		} catch (Exception ex) {
			String runtimeError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
			bw.write(" lsqrd:q"+(queryNo-1)+" lsqv:spinError \""+runtimeError+ "\" . "); }
		return queryStats;
	}

	/**
	 * Get result size of the given query
	 * @param queryStr Query
	 * @param localEndpoint Endpoint url where this query has to be executed
	 * @param sesameQueryType Query type {SELECT, ASK, CONSTRUCT, DESCRIBE}
	 * @return ResultSize
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws IOException
	 */
	public long getQueryResultSize(String queryStr,String orig_query, String localEndpoint,String sesameQueryType) throws RepositoryException, MalformedQueryException, IOException
	{
		long totalSize = -1;
		this.initializeRepoConnection(localEndpoint);
		if(sesameQueryType.equals("select") || sesameQueryType.equals("ask") )
		{
			try {
				if (sesameQueryType.equals("select"))
				{
					TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL,queryStr );
					//System.out.println(queryStr);
					tupleQuery.setMaxQueryTime(maxRunTime);
					TupleQueryResult res;
					res = tupleQuery.evaluate();
					//System.out.println(res);
					totalSize = 0;
					while(res.hasNext())
					{
						res.next();
						totalSize++;
					}
				}
				else
				{
					BooleanQuery booleanQuery = con.prepareBooleanQuery(QueryLanguage.SPARQL,queryStr );
					//System.out.println(queryStr);
					booleanQuery.setMaxQueryTime(maxRunTime);
					booleanQuery.evaluate();
					//System.out.println(res);
					totalSize = 1;

				}

			} catch (QueryEvaluationException ex) { 
				String runtimeError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
				if(runtimeError.length()>1000)  //this is to avoid sometime too big errors
					runtimeError = "Unknown runtime error";
				bw.write(" lsqv:runtimeError \""+runtimeError+ "\" ; ");
				runtimeErrorCount++;
			}
		}
		else
		{
			try {
				GraphQuery gq = con.prepareGraphQuery(QueryLanguage.SPARQL, orig_query);
				gq.setMaxQueryTime(maxRunTime);
				GraphQueryResult graphResult = gq.evaluate();
				totalSize = 0;
				while (graphResult.hasNext()) 
				{
					graphResult.next();
					totalSize++;
				}
			} catch (QueryEvaluationException ex) {
				String runtimeError = ex.getMessage().toString().replace("\"", "'").replaceAll("\n", " ").replace("\r", "");
				if(runtimeError.length()>1000)  //this is to avoid sometime too big errors
					runtimeError = "Unknown runtime error";
				bw.write(" lsqv:runtimeError \""+runtimeError+ "\" ; ");
				runtimeErrorCount++;
			}

		}
		con.close();
		return totalSize;
	}
	/**
	 * Write RDF Prefixes
	 * @param acronym Acronym of the dataset e.g. DBpedia or SWDF
	 * @throws IOException
	 */
	public void writePrefixes(String acronym) throws IOException {
		bw.write("@prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \n");
		bw.write("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . \n");
		bw.write("@prefix lsqr:<http://lsq.aksw.org/res/> . \n");
		bw.write("@prefix lsqrd:<http://lsq.aksw.org/res/"+acronym+"-> . \n");
		bw.write("@prefix lsqv:<http://lsq.aksw.org/vocab#> . \n");
		bw.write("@prefix sp:<http://spinrdf.org/sp#> . \n");
		bw.write("@prefix void:<http://rdfs.org/ns/void#> . \n");
	    bw.write("@prefix dct:<http://purl.org/dc/terms/> . \n");
	    bw.write("@prefix xsd:<http://www.w3.org/2001/XMLSchema#> . \n"); 
	    bw.write("@prefix sd:<http://www.w3.org/ns/sparql-service-description#> . \n\n");

	}
		
	/**
	 * Initialize repository for a SPARQL endpoint
	 * @param endpointUrl Endpoint Url
	 * @throws RepositoryException
	 */
	public void initializeRepoConnection(String endpointUrl) throws RepositoryException {
		Repository repo = new SPARQLRepository(endpointUrl);
		repo.initialize();
		con = repo.getConnection();

	}

}