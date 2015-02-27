
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */


import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.Boolean.*;
import static java.lang.System.out;

/****************************************************************************************
 * This class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus join.  The insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table implements Serializable
{
    /** Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /** Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key. 
     */
    private final String [] key;

    /** Index into tuples (maps key to tuple number).
     */
    private final Map <String, Comparable []> index;
    

    //----------------------------------------------------------------------------------
    // Constructors
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList <> ();        
        /* Adjust the index based on which map you want to use*/
        //index = new LinHashMap<> (String.class,Comparable[].class,11); 
       index     = new TreeMap <> ();       
       //index	  = new BpTreeMap<> (String.class, Comparable[].class); 
       //index = new ExtHashMap<> (String.class, Comparable[].class, 1024);
    } // constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuples      the list of tuples containing the data
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key,
                  List <Comparable []> _tuples)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        /* Adjust the index based on which map you want to just */
        //index = new LinHashMap<> (String.class,Comparable[].class,11); 
        index     = new TreeMap <> ();       
        //index	  = new BpTreeMap<> (String.class, Comparable[].class); 
        //index = new ExtHashMap<> (String.class, Comparable[].class, 1024);
        } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param name        the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     */
    public Table (String name, String attributes, String domains, String _key)
    {
        this (name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    //----------------------------------------------------------------------------------
    // Public Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes  the attributes to project onto
     * @return  a table of projected tuples
     */
    public Table project (String attributes)
    {
        out.println ("RA> " + name + ".project (" + attributes + ")");
        String [] attrs     = attributes.split (" ");//stores attributes into array
        Class []  colDomain = extractDom (match (attrs), domain);//extract the domain(int, string, etc.) from attrs' type
        String [] newKey    = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs; //if the list of attrs contain all keys, then newKey[]=key[]. else newKey[]=attrs[]
        //asList turns the array into a list and containsAll return true or false

        List <Comparable []> rows = new ArrayList<Comparable []>(); //list of future tuples, Comparable are basically objects that can be compared based on their key(a comparable also)
				
        int [] columnLocation = this.match(attrs);
        
        Comparable[] tempt;    //finds the correct column for each row(old tuple) and creates new tuples(rows of the desired information only)    
        int temptCount=0;       
        for(int i=0;i<tuples.size();i++){//row
        	tempt=new Comparable[attrs.length];
        	for(int i2=0;i2<domain.length;i2++){//col       		
        		for(int i3=0;i3<columnLocation.length;i3++){//if at right spot(cols), then store element from there into a tuple        			
        			if(i2==columnLocation[i3]){
        				Comparable test1=tuples.get(i)[i2];
        				tempt[temptCount]=test1;
        				temptCount++;
        				if(temptCount==attrs.length){
            				rows.add(tempt);//adds the tuple into the list	            				            				       				
            				temptCount=0;//resets the counter
            				
            				}       					        				
        			}
        		}
        		
        	}	
        	
        }
  
        List <Comparable []> rows2 = new ArrayList <> ();
        Table t = new Table (name + count++, attrs, colDomain, newKey, rows2);
        for (int i = 0; i<rows.size(); i++){
        	//t.insert will automatically add that value to the table's index as well
        	t.insert(rows.get(i));
        }
        return t;     
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t ->> t[movie.col("year")].equals (1977))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public Table select (Predicate <Comparable []> predicate)
    {
        out.println ("RA> " + name + ".select (" + predicate + ")");

        //Initialize the new list
        List <Comparable []> rows = new ArrayList <> ();
        //Loop through our current list of tuples
        for (int i = 0; i<tuples.size(); i++){
        	//If the tuple satisfies the given predicate, we want to add it to the new list of rows
        	//Otherwise, do nothing
        	if(predicate.test(tuples.get(i))){
        		rows.add(tuples.get(i));
        	}
        }
        //return a new table with the selected rows and updated index
        //Worth noting that if the list of rows is empty, the new table will still be returned, but with no rows
        List <Comparable []> rows2 = new ArrayList <> ();
        Table t = new Table (name + count++, attribute, domain, key, rows2);
        for (int i = 0; i<rows.size(); i++){
        	//t.insert will automatically add that value to the table's index as well
        	t.insert(rows.get(i));
        }
        return t;
    } // select

   /*
    /************************************************************************************
     * Select the tuples satisfying the given predicate (Integer function).
     *
     * #usage student.select  t -> t[student.col("id")].compareTo(student.col("id"))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
 /*   public Table rSelect (Predicate <Comparable []> predicate)
    {
        out.println ("RA> " + name + ".select (" + predicate + ")");
        
        return null;
    } // select
    
    */
    
    /************************************************************************************
     * Select the tuples that are in between lower bound and upper bound.
     * ex:lowerBound <= studentId <= upperBound
     * Select the tuples satisfying the given key predicate (v1<=id & id<=v2).
     *
     * #usage movie.select (int lowerBound, int upperBound)
     *
     * @param lowerBound	the lowest student id
     * @param upperBound	the highest student id
     * @return  a table with tuples in that are within that range
     */
    public Table select (int lowerBound, int upperBound)
    {
        out.println ("RA> " + name + ".select ("+lowerBound+"<=id<="+upperBound+")");
        
        //Initialize the new list
        List <Comparable []> rows = new ArrayList <> ();
        //Loop through our current list of tuples
       for (int i = 0; i<tuples.size(); i++){
        	//If the tuple in btwn, we want to add it to the new list of rows/tuples
     /*   	if(((tuples.get(i)[col("id")].compareTo(lowerBound)==0)||(tuples.get(i)[col("id")].compareTo(lowerBound)==1))&&
        	((tuples.get(i)[col("id")].compareTo(upperBound)==0)||(tuples.get(i)[col("id")].compareTo(lowerBound)==-1))){//might need have issues b/c doing tuple.compareTo(Int)
        		rows.add(tuples.get(i));
        	*/
        		
    	   int currentStudentID=(int) tuples.get(i)[col("id")];
    	   	
        		if(((currentStudentID>=lowerBound))&&(currentStudentID<=upperBound)){
        				rows.add(tuples.get(i));        	        		        		
        	}//else do nothing
        }
        //return a new table with the selected rows and updated index
        //Worth noting that if the list of rows is empty, the new table will still be returned, but with no rows
        List <Comparable []> rows2 = new ArrayList <> ();
        Table t = new Table (name + count++, attribute, domain, key, rows2);
        for (int i = 0; i<rows.size(); i++){
        	//t.insert will automatically add that value to the table's index as well
        	t.insert(rows.get(i));
        }
        return t;
    } // select
     
    
    
    /************************************************************************************
     * Select the tuples satisfying the given key predicate (v1<=id & id<=v2).  Use an index
     * (Map) to retrieve the tuple with the given key value.
     *
     * @param keyVal  the lower key value limit
     * @param UpperkeyVal the upper key value limit
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table select (KeyType keyVal, KeyType UpperkeyVal)
    {        
		out.println ("RA> " + name + ".select (" + keyVal + ")");
		
		List<Comparable[]> rows = new ArrayList<> ();
		
		//index.submap(keyVal, UpperkeyVal);
		SortedMap resultSub=((SortedMap) index).subMap(keyVal, UpperkeyVal);
		for (Object e : resultSub.entrySet()){
			//System.out.println(e);
			rows.add(index.get(e));
		}
	
		
		return new Table (name + count++, attribute, domain, key, rows);
		
		
		
    	
    } // select
    
    
    

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value).  Use an index
     * (Map) to retrieve the tuple with the given key value.
     *
     * @param keyVal  the given key value
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table select (KeyType keyVal)
    {
 
  		out.println ("RA> " + name + ".select (" + keyVal + ")");
		List<Comparable[]> rows = new ArrayList<> ();
		String newkey=new String();
		
		for (int i = 0; i < keyVal.key.length; i++)
			newkey = newkey + keyVal.key[i];
		
		rows.add (index.get(newkey));

		return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * @param table2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public Table union (Table table2)
    {
        out.println ("RA> " + name + ".union (" + table2.name + ")");
        if (! compatible (table2)) return null;

        List <Comparable []> rows = new ArrayList<Comparable []>();//initializes the list

        if(compatible(table2)){//if compatible then do the operation
        	       	
        	for(Comparable[] temp1 : tuples){//adds all tuples from table 1
            	rows.add(temp1);        	
            }
            
            for(Comparable[] temp1 : table2.tuples){//adds tuple from table 2, also checks for duplicates
            	boolean unique=true;
            	for(Comparable[] temp2 : tuples){
            		if(temp1.equals(temp2)){
            			unique=false;
            		}
            	}
            	if(unique==true){
            		rows.add(temp1);
            	}        	
            }                               
        	        	       	
        }
                
        List <Comparable []> rows2 = new ArrayList <> ();
        Table t = new Table (name + count++, attribute, domain, key, rows2);
        for (int i = 0; i<rows.size(); i++){
        	//t.insert will automatically add that value to the table's index as well
        	t.insert(rows.get(i));
        }
        return t;
    } // union

    /************************************************************************************
     * Take the difference of this table and table2.  Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * @param table2  The rhs table in the minus operation
     * @return  a table representing the difference
     * 
     */
    public Table minus (Table table2)
    {
        out.println ("RA> " + name + ".minus (" + table2.name + ")");
        if (! compatible (table2)) return null;

        List <Comparable []> rows = new ArrayList <Comparable []> () ;
    	
        for(Comparable [] newrows : tuples){
        	boolean check = true;
        	for(Comparable [] row : table2.tuples){
        		if(newrows.equals(row)){
        			check = false;
        			break;
        		}
        	}
        	if(check)
        	rows.add(newrows);
        	     	       	
        }

        // I M P L E M E N T E D 

        List <Comparable []> rows2 = new ArrayList <> ();
        Table t = new Table (name + count++, attribute, domain, key, rows2);
        for (int i = 0; i<rows.size(); i++){
        	//t.insert will automatically add that value to the table's index as well
        	t.insert(rows.get(i));
        }
        return t;
    } // minus

    /************************************************************************************
     * CompareAttr is used in our join methods
     */
    public boolean compareAttr(String[] attr1, String[] attr2, Comparable[] row1, Comparable[] row2, Table table2){
    	
        int [] newattr1 = this.match(attr1);
        int [] newattr2 = table2.match(attr2);
    	boolean check = true;
    	
		for(int i=0; i < attr1.length;i++){
			if(!(row1[newattr1[i]].equals(row2[newattr2[i]]))){
				check = false;
				break;
			}//if
		}//for
		
		return check;
    }
    /************************************************************************************
     * Join this table and table2 by performing an equijoin.  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.
     *
     * #usage movie.join ("studioNo", "name", studio)
     * #usage movieStar.join ("name == s.name", starsIn)
     * @param attribute1  the attributes of this table to be compared (Foreign Key)
     * @param attribute2  the attributes of table2 to be compared (Primary Key)
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {
        out.println ("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", "
                                               + table2.name + ")");

        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");

        List <Comparable []> rows = new ArrayList <Comparable []> () ;

	    for(Comparable [] newrows : tuples){
	    	for(Comparable [] row : table2.tuples){
	        		if(compareAttr(t_attrs, u_attrs, newrows, row, table2))
	                	rows.add(ArrayUtil.concat(newrows, row));
	        }
        }
	    
	    return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                ArrayUtil.concat (domain, table2.domain), key, rows);

    }
    
    /************************************************************************************
     * Join this table and table2 by performing an equijoin in respect to their indexes.
     * The primary and foreign keys are compared to see if they are equal.  
     * Tuples from both tables are compared requiring attributes1 to equal attributes2. 
     * 
     * @param keyVal  the key of the first table
     * @param keyVal2  the key of the second table
     * @return  a table with the joined index tuples
     */
    public Table indexedJoin (String attributes1, String attributes2, Table table2)
    {
        out.println ("RA> " + name + ".indexedjoin (" + attributes1 + ", " + attributes2 + ", "
                + table2.name + ")");
        
        String [] attr1 = attributes1.split (" ");
        String [] attr2 = attributes2.split (" ");
        
    	//first see which one is a primary key
    	KeyType key1 = new KeyType(attributes1);
    	KeyType key2 = new KeyType(attributes2);
    	Table fTable = null;
    	Table pTable = null;
    	
    	if (key1.equals(new KeyType(key))){
    		//primary key of table 1
    		fTable = table2;
    		pTable = this;
    		
    	}
    	else if (key2.equals(new KeyType(table2.key))){
    		//primary key of table 2
    		fTable = this;
    		pTable = table2;
    	}

        List <Comparable []> rows = new ArrayList <Comparable []> ();

	    for(Comparable [] newrows: fTable.tuples){
	    	for(Comparable [] row : pTable.tuples){
        		if(compareAttr(attr1, attr2, newrows, row, table2))
                	rows.add(ArrayUtil.concat(row, newrows));	
        	}
        }
	    
		return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                ArrayUtil.concat (domain, table2.domain), key, rows);
		
    }
    
    /************************************************************************************
     * Return the column position for the given attribute name.
     *
     * @param attr  the given attribute name
     * @return  a column position
     */
    public int col (String attr)
    {
        for (int i = 0; i < attribute.length; i++) {
           if (attr.equals (attribute [i])) return i;
        } // for

        return -1;  // not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup  the array of attribute values forming the tuple
     * @return  whether insertion was successful
     */
    public boolean insert (Comparable [] tup)
    {
        //out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            String newkey=new String();
            
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++){
            	keyVal [j] = tup [cols [j]];
            	newkey=newkey+keyVal[j];
        	}
            index.put(newkey, tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /************************************************************************************
     * Get the name of the table.
     *
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName

    /************************************************************************************
     * Print this table.
     */
    public void print ()
    {
		if(!this.tuples.isEmpty ())
		{
			out.println ("\n Table " + name);
			out.print ("|-");
			for (int i = 0; i < attribute.length; i++)
				out.print ("---------------");
			out.println ("-|");
			out.print ("| ");
			for (String a : attribute)
				out.printf ("%15s", a);
			out.println (" |");
			out.print ("|-");
			for (int i = 0; i < attribute.length; i++)
				out.print ("---------------");
			out.println ("-|");
			for (Comparable[] tup : tuples)
			{
				out.print ("| ");
				for (Comparable attr : tup)
					out.printf ("%15s", attr);
				out.println (" |");
			} // for
			out.print ("|-");
			for (int i = 0; i < attribute.length; i++)
				out.print ("---------------");
			out.println ("-|");
		}
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        for (Map.Entry <String, Comparable []> e : index.entrySet ()) {
            out.println (e.getKey () + " -> " + Arrays.toString (e.getValue ()));
        } // for
        out.println ("-------------------");
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory. 
     *
     * @param name  the name of the table to load
     */
    public static Table load (String name)
    {
        Table tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream (new FileInputStream (DIR + name + EXT));
            tab = (Table) ois.readObject ();
            ois.close ();
        } catch (IOException ex) {
            out.println ("load: IO Exception");
            ex.printStackTrace ();
        } catch (ClassNotFoundException ex) {
            out.println ("load: Class Not Found Exception");
            ex.printStackTrace ();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save ()
    {
        try {
            ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (DIR + name + EXT));
            oos.writeObject (this);
            oos.close ();
        } catch (IOException ex) {
            out.println ("save: IO Exception");
            ex.printStackTrace ();
        } // try
    } // save

    //----------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (Table table2)
    {
        if (domain.length != table2.domain.length) {
            out.println ("compatible ERROR: table have different arity");
            return false;
        } // if
        for (int j = 0; j < domain.length; j++) {
            if (domain [j] != table2.domain [j]) {
                out.println ("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column  the array of column names
     * @return  an array of column index positions
     */
    private int [] match (String [] column)
    {
        int [] colPos = new int [column.length];

        for (int j = 0; j < column.length; j++) {
            boolean matched = false;
            for (int k = 0; k < attribute.length; k++) {
                if (column [j].equals (attribute [k])) {
                    matched = true;
                    colPos [j] = k;
                } // for
            } // for
            if ( ! matched) {
                out.println ("match: domain not found for " + column [j]);
            } // if
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t       the tuple to extract from
     * @param column  the array of column names
     * @return  a smaller tuple extracted from tuple t 
     */
    private Comparable [] extract (Comparable [] t, String [] column)
    {
        Comparable [] tup = new Comparable [column.length];
        int [] colPos = match (column);
        for (int j = 0; j < column.length; j++) tup [j] = t [colPos [j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the type of
     * each value to ensure it is from the right domain. 
     *
     * @param t  the tuple as a list of attribute values
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     */
    private boolean typeCheck (Comparable [] t)
    { 
    	Class c;
    	
    	//Check size of tuple
        if (t.length != domain.length) {
            out.println ("size ERROR: tuple size does not match domain");
            return false;
        } // if

        //Check type of each value in tuple
        for (int j = 0; j < t.length; j++) {
        	c = t[j].getClass();	//Get type of t[j]
        	if (!(c.equals(domain[j]))){
                out.println("type ERROR: expected type of tuple is" + domain[j]);
        		out.println("tuple type is: " + c);
                return false;
            } // if
        } // for

        return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos the column positions to extract.
     * @param group  where to extract from
     * @return  the extracted domains
     */
    private Class [] extractDom (int [] colPos, Class [] group)
    {
        Class [] obj = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            obj [j] = group [colPos [j]];
        } // for

        return obj;
    } // extractDom

} // Table class
