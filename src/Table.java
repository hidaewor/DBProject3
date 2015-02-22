
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */


import java.io.*;
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
    private final Map <KeyType, Comparable []> index;

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
        index     = new TreeMap <> ();       // also try BPTreeMap, LinHashMap or ExtHashMap
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
        index     = new TreeMap <> ();       // also try BPTreeMap, LinHashMap or ExtHashMap
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

        //  T O   B E   I M P L E M E N T E D 				
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
        	out.println("here");
        	t.insert(rows.get(i));
        }
        return t;
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

      //Initialize the new list
        List <Comparable []> rows = new ArrayList <> ();

        //Get a set of the index. This is necessary for iteration of a treemap
        Set set = index.entrySet();
        // Get an iterator for the set
        Iterator i = set.iterator();
        // Loop through the index
        while(i.hasNext()) { 
        	 Map.Entry entry = (Map.Entry)i.next();
        	 KeyType aKey = (KeyType)entry.getKey();
        	 //If the key of the entry matches the key value we're looking for, add it to the new list of rows
        	 if(aKey.equals(keyVal)){
        		 rows.add((Comparable[]) entry.getValue());
        	 }
        }
       
      //return a new table with the selected rows
        //Worth noting that if the list of rows is empty, the new table will still be returned, but with no rows
        List <Comparable []> rows2 = new ArrayList <> ();
        Table t = new Table (name + count++, attribute, domain, key, rows2);
        for (int j = 0; j<rows.size(); j++){
        	//t.insert will automatically add that value to the table's index as well
        	t.insert(rows.get(j));
        }
        return t;
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

        //  T O   B E   I M P L E M E N T E D 
        
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
        
        // I M P L E M E N T E D 
        int [] newattr1 = this.match(t_attrs);
        int [] newattr2 = table2.match(u_attrs);
        for(Comparable [] newrows : tuples){
        	
        	for(Comparable [] row : table2.tuples){
        		boolean check = true;
        		for(int i=0;i<newattr1.length;i++){
        			if(!(newrows[newattr1[i]].equals(row[newattr2[i]]))){
        				check = false;
        				break;
        			}
        		}
        		if(check)
                	rows.add(ArrayUtil.concat(newrows, row));
        	}
        
        	     	       	
        }
        
        List <Comparable []> rows2 = new ArrayList <> ();
        Table t = new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                ArrayUtil.concat (domain, table2.domain), key, rows2);
        for (int i = 0; i<rows.size(); i++){
        	//t.insert will automatically add that value to the table's index as well
        	t.insert(rows.get(i));
        }
  
        return t;
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
        out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            index.put (new KeyType (keyVal), tup);
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
        out.println ("\n Table " + name);
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        for (Map.Entry <KeyType, Comparable []> e : index.entrySet ()) {
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
        	
        	/*If tuple type is a double, make it a float 
        	 * since getClass does not recognize 10.0 as float
        	 
        	if (t[j] instanceof Double){
				try {
					c = Class.forName("java.lang.Float");
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
        	}*/
       
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
