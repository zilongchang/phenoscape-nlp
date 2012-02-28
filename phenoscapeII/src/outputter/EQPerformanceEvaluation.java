/**
 * 
 */
package outputter;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * @author Hong Updates
 * compare raw EQ table to EQ answer table
 * obtain precision/recall measurements 
 */
public class EQPerformanceEvaluation {

	private Connection conn;
	private String username ="root";
	private String password = "root";
	private String testtable;
	private String answertable;
	private String prtablefields;
	private String prtableEQs;
	private String prtableTranslations;
	private boolean printfields = true;
	private boolean printEQs = true;
	private boolean printTranslations = true;
	
	//init
	Hashtable<String, String> counts;
	ArrayList<String> fields = new ArrayList<String>();

	/**
	 * 
	 */
	public EQPerformanceEvaluation(String database, String testtable, String answertable, String prtable) {
		this.testtable = testtable;
		this.answertable = answertable;
		this.prtableEQs = prtable+"_EQs";
		this.prtablefields = prtable+"_fields";
		this.prtableTranslations = prtable+"_translations";
		initFields();
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();

				String sql ="create table if not exists "+prtablefields+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"entityp float(4,2), entityr float(4,2), " +
						"entitylabelp float(4,2), entitylabelr float(4,2), " +
						"entityidp float(4,2), entityidr float(4,2), " +
						"qualityp float(4,2), qualityr float(4,2), " +
						"qualitylabelp float(4,2), qualitylabelr float(4,2), " +
						"qualityidp float(4,2), qualityidr float(4,2), " +
						"qualitynegatedp float(4,2), qualitynegatedr float(4,2), " +
						"qualitynegatedlabelp float(4,2), qualitynegatedlabelr float(4,2), " +
						"qnparentlabelp float(4,2), qnparentlabelr float(4,2), " +
						"qnparentidp float(4,2), qnparentidr float(4,2), " +
						"qualitymodifierp float(4,2), qualitymodifierr float(4,2), " +
						"qualitymodifierlabelp float(4,2), qualitymodifierlabelr float(4,2), " +
						"qualitymodifieridp float(4,2), qualitymodifieridr float(4,2), " +
						"entitylocatorp float(4,2), entitylocatorr float(4,2), " +
						"entitylocatorlabelp float(4,2), entitylocatorlabelr float(4,2), " +
						"entitylocatoridp float(4,2), entitylocatoridr float(4,2), " +
						"counttp float(4,2), counttr float(4,2) " +
						")";
				stmt.execute(sql);
				
				stmt.execute("create table if not exists "+prtableEQs+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"rawexactp float(4,2), rawexactr float(4,2), " +
						"rawpartialp float(4,2), rawpartialr float(4,2), " +
						"formalexactp float(4,2), formalexactr float(4,2), " +
						"formalpartialp float(4,2), formalpartialr float(4,2) "+
						")");

				stmt.execute("create table if not exists "+this.prtableTranslations+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"entityp float(4,2), entityr float(4,2), " +
						"qualityp float(4,2),qualityr float(4,2) " +
						")");
				}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void initFields() {
		this.fields.add("entity");
		this.fields.add("entitylabel");
		this.fields.add("entityid");
		this.fields.add("quality");
		this.fields.add("qualitylabel");
		this.fields.add("qualityid");
		this.fields.add("qualitynegated");
		this.fields.add("qualitynegatedlabel");
		this.fields.add("qnparentlabel");
		this.fields.add("qnparentid");
		this.fields.add("qualitymodifier");
		this.fields.add("qualitymodifierlabel");
		this.fields.add("qualitymodifierid");
		this.fields.add("entitylocator");
		this.fields.add("entitylocatorlabel");
		this.fields.add("entitylocatorid");
		this.fields.add("countt");		
	}

	/**
	 * get precision and recall measurements
	 * precision = #matched/#generated
	 * recall = #matched/#inanswer
	 */
	public void evaluate(){	
		ArrayList<String> states = new ArrayList<String>(); 
		//tallying 
		try{
			//collect all unique state ids
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct characterid, stateid from "+this.answertable);
			while(rs.next()){
				String characterid = rs.getString("characterid");
				String stateid = rs.getString("stateid");
				if(characterid.length() > 0 && stateid.length() > 0){
					states.add(stateid);
				}
			}
			//pair up answer and test states
			ArrayList<ArrayList<Hashtable<String,String>>> astates = new ArrayList<ArrayList<Hashtable<String,String>>>();
			ArrayList<ArrayList<Hashtable<String,String>>> tstates = new ArrayList<ArrayList<Hashtable<String,String>>>();			
			Iterator<String> it = states.iterator();
			while(it.hasNext()){
				String stateid = it.next();
				ArrayList<Hashtable<String, String>> astate = new ArrayList<Hashtable<String, String>>();
				rs = stmt.executeQuery("select entity, entitylabel, entityid, quality, qualitylabel, qualityid, qualitynegated, qualitynegatedlabel, qnparentlabel, qnparentid, qualitymodifier, qualitymodifierlabel, qualitymodifierid, entitylocator, entitylocatorlabel, entitylocatorid, countt from "+
					this.answertable+" where stateid = '"+stateid+"'");
				while(rs.next()){
					Hashtable<String, String> EQ = new Hashtable<String, String> ();
					for(String field: this.fields){
						String v = rs.getString(field);
						EQ.put(field, v);
					}		
					astate.add(EQ);
				}
				System.out.println("added ["+stateid+"] from answer");
				astates.add(astate);
				ArrayList<Hashtable<String, String>> tstate = new ArrayList<Hashtable<String, String>>();
				rs = stmt.executeQuery("select entity, entitylabel, entityid, quality, qualitylabel, qualityid, qualitynegated, qualitynegatedlabel, qnparentlabel, qnparentid, qualitymodifier, qualitymodifierlabel, qualitymodifierid, entitylocator, entitylocatorlabel, entitylocatorid, countt from "+
					this.testtable+" where stateid = '"+stateid+"'");
				while(rs.next()){
					Hashtable<String, String> EQ = new Hashtable<String, String> ();
					for(String field: this.fields){
						String v = rs.getString(field);
						EQ.put(field, v);
					}
					tstate.add(EQ);
				}
				tstates.add(tstate);
				System.out.println("added ["+stateid+"] from test");
			}
			compareFields(tstates, astates);//precision and recall for each of the fields
			compareEQs(tstates, astates); //for raw/labeled EQ statements
			compareTranslations(); //translation from raw to labels/Ids
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	/****************************************************  Fields  *************************************
	 * 
	 * @param tstate
	 * @param astate
	 */
	private void compareFields(ArrayList<ArrayList<Hashtable<String, String>>> tstates,
			ArrayList<ArrayList<Hashtable<String, String>>> astates) {
		if(counts == null){
			 counts = new Hashtable<String, String> ();
			 //init
			for(String field : this.fields){
					counts.put("inanswer"+field, ""+0);		
					counts.put("generated"+field, ""+0);
					counts.put("matched"+field, ""+0);
			}
			//put totals in
			for(String field : this.fields){
				getTotal(field);
			}			
		}
		

		//collecting matched field by field
		for(String field : this.fields){
			int wcount = 0;
			int tcount = 0;
			int acount = 0;
			for(int i = 0; i < astates.size(); i++){
				ArrayList<String> avalues = new ArrayList<String>();
				for(Hashtable<String, String> EQ :astates.get(i)){
					String v = EQ.get(field);
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*,\\s*");
						for(String v1 : vs){
							if(v1.length()>0) avalues.add(v1);
						}
					}
				}	
				
							
				ArrayList<String> tvalues = new ArrayList<String>();
				for(Hashtable<String, String> EQ :tstates.get(i)){
					String v = EQ.get(field);
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*,\\s*");
						for(String v1 : vs){
							if(v1.length()>0)tvalues.add(v1);
						}
					}
				}
				
				if(field.compareTo("entity")==0){
					tcount += tvalues.size();
					acount += avalues.size();
					System.out.println("t +"+tvalues.size()+"at state "+i);
					System.out.println("a +"+avalues.size()+"at state "+i);
				}
				for(int j = 0; j<tvalues.size(); j++){
					for(int k = 0; k < avalues.size(); k++){
						String v = tvalues.get(j);
						String a = avalues.get(k);
						if(a.length()>0 && v.length()>0 && (a.contains(v) || v.contains(a))){
							if(this.printfields && field.compareTo("entity")==0){								
								wcount++;
								System.out.println("total t:"+tcount);
								System.out.println("total a:"+acount);
								System.out.println("total m:"+wcount);
								System.out.println(wcount+":["+v+"]=["+avalues.toString()+"]");
								
							}
							counts.put("matched"+field, ""+(Integer.parseInt(counts.get("matched"+field))+1));
							tvalues.set(j, ""); //remove matched
							avalues.set(k, "");
							break;
						}else{ //not matched
							//if(this.printfields && field.compareTo("entity")==0){
							//	System.out.println("["+v+"]x["+avalues.toString()+"]");
							//}
						}
					}
				}
			}
			if(field.compareTo("entity")==0){
				System.out.println("total t:"+tcount);
				System.out.println("total a:"+acount);
				System.out.println("total m:"+wcount);
			}
		}

		//calculate and output P/R measurements
		String prstring = ""; 
		String fieldstring = "";
		for(String field : this.fields){
			fieldstring += field+"p,"+field+"r,";
			float p = Integer.parseInt(counts.get("generated"+field))==0? -1 : (float)Integer.parseInt(counts.get("matched"+field))/Integer.parseInt(counts.get("generated"+field));
			float r = Integer.parseInt(counts.get("inanswer"+field)) ==0? -1 : (float)Integer.parseInt(counts.get("matched"+field))/Integer.parseInt(counts.get("inanswer"+field));
			prstring += p+","+r+",";
		}
		prstring = prstring.replaceFirst(",$", "");
		fieldstring = fieldstring.replaceFirst(",$", "");
		insertInto(this.prtablefields, fieldstring, prstring);
	}

	/**
	 * 
	 * @param prtablefields2
	 */
	private void insertInto(String tablename, String fieldstring, String prstring) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+tablename+"("+fieldstring+")"+" values ("+prstring+")");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * count only the fields associated with a state statement
	 * @param field
	 */
	private void getTotal(String field) {
		try{
			Statement stmt = conn.createStatement();
			//total for answers
			int count = 0;
			ResultSet rs = stmt.executeQuery("select "+field+" from "+this.answertable+" where "+field+" is not null and length(trim("+field+"))>0 and length(stateid)>0");
			while(rs.next()){
				String[] tokens = rs.getString(1).trim().split("\\s*,\\s*");
				for(String t : tokens){
					if(t.length()>0) count++;
				}
			}
			counts.put("inanswer"+field, ""+(count++));
			
			//total for generated
			count = 0;
			rs = stmt.executeQuery("select "+field+" from "+this.testtable+" where "+field+" is not null and length(trim("+field+"))>0 and length(stateid)>0");
			while(rs.next()){
				String[] tokens = rs.getString(1).trim().split("\\s*,\\s*");
				for(String t : tokens){
					if(t.length()>0) count++;
				}
			}
			counts.put("generated"+field, ""+(count++));

		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/****************************************************  EQs  *************************************
	 * 
	 * @param tstate: EQs generated by the algorithm for a state
	 * @param astate: EQs in answer key for a state
	 */
	private void compareEQs(ArrayList<ArrayList<Hashtable<String, String>>> tstates,
			ArrayList<ArrayList<Hashtable<String, String>>> astates) {
		//raw
		int totalrawgenerated = 0;
		int totalrawinanswer = 0;
		int partialrawmatches = 0;
		int exactrawmatches = 0;
		String prstring = "";
		String fieldstring = "";
		for(int i = 0; i<astates.size(); i++){
			totalrawinanswer += astates.get(i).size();
			totalrawgenerated += tstates.get(i).size();
			for(Hashtable<String, String> tEQ : tstates.get(i)){
				String entity = tEQ.get("entity");
				String entitylocator = tEQ.get("entitylocator");
				String qualitymodifier = tEQ.get("qualitymodifier");
				String quality = tEQ.get("quality");
				if(quality==null || quality.length()==0){
					quality = tEQ.get("qualitynegated");
				}
				int[] matchingresult = matchAstates(entity, entitylocator, quality, qualitymodifier, astates.get(i), "");
				exactrawmatches += matchingresult[0];
				partialrawmatches += matchingresult[0] + matchingresult[1];
			}
		}
		
		fieldstring+= "rawexactp, rawexactr, rawpartialp, rawpartialr,";
		float exactrawp = totalrawgenerated==0? -1 : (float)exactrawmatches/totalrawgenerated;
		float exactrawr = totalrawinanswer==0? -1 : (float)exactrawmatches/totalrawinanswer;
		float partialrawp = totalrawgenerated ==0? -1 : (float)partialrawmatches/totalrawgenerated;
		float partialrawr = totalrawinanswer ==0? -1 : (float)partialrawmatches/totalrawinanswer;
		prstring += exactrawp +","+ exactrawr +"," +
				partialrawp +","+ partialrawr+",";
		
		//labeled
		int totallabeledgenerated = 0;
		int totallabeledinanswer = 0;
		int partiallabeledmatches = 0;
		int exactlabeledmatches = 0;

		for(int i = 0; i<astates.size(); i++){
			totallabeledinanswer += astates.get(i).size();
			totallabeledgenerated += tstates.get(i).size();
			for(Hashtable<String, String> tEQ : tstates.get(i)){
				String entity = tEQ.get("entitylabel");
				String entitylocator = tEQ.get("entitylocatorlabel");
				String qualitymodifier = tEQ.get("qualitymodifierlabel");
				String quality = tEQ.get("qualitylabel");
				if(quality==null || quality.length()==0){
					quality = tEQ.get("qualitynegatedlabel");
				}
				int[] matchingresult = matchAstates(entity, entitylocator, quality, qualitymodifier, astates.get(i), "label");
				exactlabeledmatches += matchingresult[0];
				partiallabeledmatches += matchingresult[0] + matchingresult[1];
			}
		}		
		fieldstring+= "formalexactp, formalexactr, formalpartialp, formalpartialr";
		
		float exactlabeledp = totallabeledgenerated==0? -1 : (float)exactlabeledmatches/totallabeledgenerated;
		float exactlabeledr = totallabeledinanswer==0? -1 : (float)exactlabeledmatches/totallabeledinanswer;
		float partiallabeledp = totallabeledgenerated ==0? -1 : (float)partiallabeledmatches/totallabeledgenerated;
		float partiallabeledr = totallabeledinanswer ==0? -1 : (float)partiallabeledmatches/totallabeledinanswer;
		prstring += exactlabeledp +","+ exactlabeledr +"," + partiallabeledp +","+ partiallabeledr;

		this.insertInto(this.prtableEQs, fieldstring, prstring);
	}
	
	
	
	/**
	 * match the set of 4 values to EQs in aState
	 * the match EQ is removed from aState
	 * @param entity
	 * @param entitylocator
	 * @param quality
	 * @param qualitymodifier
	 * @param aState
	 * @param postfix
	 * @return 2-element int array: the first element is 1 (0) if there is (not) an exact match, the second element is 1 (0) if there is (not) an partial match
	 */
	private int[] matchAstates(String entity, String entitylocator,
			String quality, String qualitymodifier,
			ArrayList<Hashtable<String, String>> aState, String suffix) {
		int[] matches = new int[]{0, 0};
		int matchsize = 0;
		int index = -1;
		for(int i = 0; i < aState.size(); i++){
			int matchedfields = matchInState(entity, entitylocator, quality, qualitymodifier, aState.get(i), suffix);
			if(matchedfields > matchsize){
				matchsize = matchedfields;
				index = i;
			}
		}
		if(matchsize == 4){
			matches[0] = 1;
			matches[1] = 0;
			aState.remove(index);
			return matches;
		}else if(matchsize >=2 && matchsize < 4){
			matches[0] = 0;
			matches[1] = 1;
			aState.remove(index);
			return matches;
		}else{
			return matches;
		}
	}
	
	
	
	/**
	 * 
	 * @param entity
	 * @param entitylocator
	 * @param quality
	 * @param qualitymodifier
	 * @param EQ
	 * @param suffix
	 * @return 0 if entity and/or quality not match, otherwise, 3 to indicate a partial match, 4 an exact match of all 4 fields 
	 */
	private int matchInState(String entity, String entitylocator,
			String quality, String qualitymodifier,
			Hashtable<String, String> EQ, String suffix) {
		String e = EQ.get("entity"+suffix);
		String q = EQ.get("quality"+suffix);
		int exact = 0;
		int partial = 0;
		if(!e.contains(entity) && !entity.contains(e) && !q.contains(quality) && !quality.contains(q)){
			return 0;
		}
		if(e.length()==0 || q.length()==0 || entity.length()==0 || quality.length()==0) return 0;
		
		if(e.compareTo(entity) ==0 && e.length()!=0 && entity.length()!=0 ){
			exact++;
		}else if((e.contains(entity) || entity.contains(e)) && e.length()!=0 && entity.length()!=0){
			partial++;
		}
		
		if(q.compareTo(quality)==0 && q.length()!=0 && quality.length()!=0){
			exact++;
		}else if((q.contains(quality) || quality.contains(q)) && q.length()!=0 && quality.length()!=0){
			partial++;
		}
		
		//compare qualitymodifier
		if(qualitymodifier.length()>0 && EQ.get("qualitymodifier"+suffix).length()>0){
			String[] aqms = EQ.get("qualitymodifier"+suffix).split("\\s*,\\s*");
			String[] tqms = qualitymodifier.split("\\s*,\\s*");
			
			int thisexact = 0;
			int thispartial = 0;
			for(int i = 0; i < aqms.length; i++){
				for(int j = 0; j <tqms.length; j++){
					if(aqms[i].compareTo(tqms[j])==0){
						thisexact++;
					}else if(aqms[i].contains(tqms[j]) && tqms[j].contains(aqms[i])){
						thispartial++;
					}
				}
			}
			if(aqms.length != tqms.length || (thispartial+thisexact > 0 && thisexact != aqms.length)){
				partial++;
			}else if(aqms.length == tqms.length && thisexact == aqms.length){
				exact++; 
			}
		}
		//compare entitylocator
		if(entitylocator.length()>0 && EQ.get("entitylocator"+suffix).length()>0){
			String[] tels = entitylocator.split("\\s*,\\s*");
			String[] aels = EQ.get("entitylocator"+suffix).split("\\s*,\\s*");
			int thisexact = 0;
			int thispartial = 0;
			for(int i = 0; i < aels.length; i++){
				for(int j = 0; j <tels.length; j++){
					if(aels[i].compareTo(tels[j])==0){
						thisexact++;
					}else if(aels[i].contains(tels[j]) && tels[j].contains(aels[i])){
						thispartial++;
					}
				}
			}
			if(aels.length != tels.length || (thispartial+thisexact > 0 && thisexact != aels.length)){
				partial++;
			}else if(aels.length == tels.length && thisexact == aels.length){
				exact++; 
			}
		}
		return exact+partial;
	}

	/*************************************************  translations  *****************************************
	 * measures P/R for entities and qualities translations from raw to label
	 * entities: entity, entitylocator, qualitymodifier
	 * qualities: quality
	 * @param tstate
	 * @param astate
	 */
	private void compareTranslations() {
		//select unique pairs of raw and labeled values from database 
		ArrayList<String> entities = new ArrayList<String>();
		entities.add("entity, entitylabel");
		entities.add("entitylocator, entitylocatorlabel");
		entities.add("qualitymodifier, qualitymodifierlabel");
		
		ArrayList<String> qualities = new ArrayList<String>();
		qualities.add("quality, qualitylabel");
		
		Hashtable<String, String> aentitytranslations = new Hashtable<String, String>();
		Hashtable<String, String> tentitytranslations = new Hashtable<String, String>();
		Hashtable<String, String> aqualitytranslations = new Hashtable<String, String>();
		Hashtable<String, String> tqualitytranslations = new Hashtable<String, String>();
		try{
			//collecting entity pairs
			for(String entitypair : entities){
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct "+entitypair+" from "+this.answertable+" where length(stateid)>0");
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						String raw = raws[i].trim();
						String label = i<labels.length ? labels[i].trim(): "";
						if(raw.length() != 0){
							aentitytranslations.put(raw, label);
						}
					}
				}				
				rs = stmt.executeQuery("select distinct "+entitypair+" from "+this.testtable + " where length(stateid)>0");
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = new String[raws.length];
					labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						String raw = raws[i].trim();
						String label = i<labels.length? labels[i].trim(): "";
						if(raw.length() != 0){
							tentitytranslations.put(raw, label);
						}
					}
				}											
			}
			//collecting quality pairs
			for(String qualitypair : qualities){
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct "+qualitypair+" from "+this.answertable + " where length(stateid)>0");
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						aqualitytranslations.put(raws[i].trim(), i<labels.length? labels[i].trim() : "");
					}
				}				
				rs = stmt.executeQuery("select distinct "+qualitypair+" from "+this.testtable + " where length(stateid)>0");
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						tqualitytranslations.put(raws[i].trim(), i<labels.length? labels[i].trim() : "");
					}
				}											
			}
			//matching and calculating entity translations
			String prstring ="";
			String fieldstring = "";
			int totalentitypairgenerated = tentitytranslations.size();
			int totalentitypairinanswer = aentitytranslations.size();
			int matchedentities = 0;
			Enumeration<String> trawentities = tentitytranslations.keys();
			while(trawentities.hasMoreElements()){
				String traw = trawentities.nextElement();
				String tlabel = tentitytranslations.get(traw);
				String alabel = aentitytranslations.get(traw);
				if(alabel !=null && tlabel !=null && alabel.compareToIgnoreCase(tlabel)==0){
					matchedentities++;
				}
			}
			fieldstring +="entityp, entityr,";
			float p = totalentitypairgenerated ==0? -1 : (float)matchedentities/totalentitypairgenerated;
			float r = totalentitypairinanswer ==0? -1 : (float)matchedentities/totalentitypairinanswer;
			prstring += p + ","+ r+",";			
			int totalqualitypairgenerated = tqualitytranslations.size();
			int totalqualitypairinanswer = aqualitytranslations.size();
			int matchedqualities = 0;
			Enumeration<String> trawqualities = tqualitytranslations.keys();
			while(trawqualities.hasMoreElements()){
				String traw = trawqualities.nextElement();
				String tlabel = tqualitytranslations.get(traw);
				String alabel = aqualitytranslations.get(traw);
				if(alabel !=null && tlabel !=null && alabel.compareToIgnoreCase(tlabel)==0){
					matchedqualities++;
				}
			}
			fieldstring +="qualityp, qualityr";
			p = totalqualitypairgenerated ==0? -1 : (float)matchedqualities/totalqualitypairgenerated;
			r = totalqualitypairinanswer ==0? -1 : (float)matchedqualities/totalqualitypairinanswer;
			prstring += p + ","+ r;
			this.insertInto(this.prtableTranslations,fieldstring, prstring);			
		}catch(Exception e){
				e.printStackTrace();
		}				
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String database = "biocreative2012";
		String testtable = "run0";
		String answertable = "internalworkbench";
		String prtable = "evaluationrecords";
		EQPerformanceEvaluation pe = new EQPerformanceEvaluation(database, testtable, answertable, prtable);
		pe.evaluate();
		

	}

}
