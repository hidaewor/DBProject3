/*****************************************************************************************
 * @file  Tester.java
 * @author Hope Idaewor
 */

import static java.lang.System.out;
/*****************************************************************************************
 * This class times our select and joins with the Student Registration Database defined in the
 * Kifer, Bernstein and Lewis 2006 database textbook.
 * Based off of the TestTupleGenerator given.
 */
public class Tester {
    /*************************************************************************************
     * The gen method generates tuples and adds them to tables in order to test Select and Join
     * @param args  the command-line arguments
     */
	public static void gen(){
	
	long startTime, endTime, duration;
	
	/* Table Generator */
    TupleGenerator test = new TupleGeneratorImpl ();

    test.addRelSchema ("Student",
                       "id name address status",
                       "Integer String String String",
                       "id",
                       null);
    
    test.addRelSchema ("Professor",
                       "id name deptId",
                       "Integer String String",
                       "id",
                       null);

    /* Create Tables */
    Table student = new Table("student", "id name address status", "Integer String String String", "id");
    Table professor = new Table("professor","id name deptId", "Integer String String","id");
    String [] tables = { "Student", "Professor"};
    
    /* Insert Tuples */
    int ntups [] = new int [] {100,100}; //student = 3, professor = 3
    Comparable[][][] tups = test.generate(ntups);
    out.println("DDL> Inserting 5000 student tuples and 2000 professor tuples...");
    for (int i = 0; i <tups.length; i++) {
        for (int j = 0; j < tups[i].length; j++) {
        	if (tables[i].equals("Student")){
                	student.insert(tups[i][j]);
                }
            else{
                	professor.insert(tups[i][j]);
                }
        } // for
    } // for    
    
    //Print tables
    student.print();
    professor.print(); 


    /* Case 1: Select Point Query */
    
    //--------------------- no index select
    out.println ();
    out.println("----Case 1.1: Select Point Query, No Index----");
    startTime = System.currentTimeMillis();
    Table t_select = student.select (t -> t[student.col("status")].equals ("status437613"));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("No Index time = " + duration + " ms");
    t_select.print ();
 
    //--------------------- indexed select (currently using TreeMap, change in Table.java constructors)
    out.println ();
    out.println("----Case 1.2: Select Point Query, Indexed----");
    startTime = System.currentTimeMillis();
    Table t_iselect = student.select (new KeyType (96750));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("Indexed Select time = " + duration + " ms");
    t_iselect.print ();


 
   /* Case 2: Select Range Query 
    * Problems: How do we actually put a range in select??
    * 
    
    //--------------------- no index select
    out.println ();
    out.println("----Case 2.1: Select Range Query, No Index----");
    startTime = System.currentTimeMillis();
    Table t_rselect = student.select (t -> t[student.col("id")].compareTo(o));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("No Index time = " + duration + " ms");
    t_rselect.print ();
 
    //--------------------- indexed select (currently using TreeMap, change in Table.java)
    out.println ();
    out.println("----Case 2.2: Select Range Query, Indexed----");
    startTime = System.currentTimeMillis();
    Table t_riselect = student.select (new KeyType (680080));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("Indexed Select time = " + duration + " ms");
    t_riselect.print ();
    
    */
     
  
   /* Case 3: Join 
    * Problems: How do we do an indexed join? 
    *			Also, the no index join is not returning anything because 
    *			none of the tuples are equal since they are randomly generated.
    * */

    //--------------------- no index join
    out.println ();
    out.println("----Case 3.1: Join, No Index----");
    startTime = System.currentTimeMillis();
    Table t_jselect = student.join ("name", "address", professor);
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("No Index time = " + duration + " ms");
    t_jselect.print ();
    
    //--------------------- indexed join (currently using TreeMap, change in Table.java)
    out.println ();
    out.println("----Case 3.2: Join, Indexed----");
    startTime = System.currentTimeMillis();
    Table t_jiselect = student.indexedJoin ("980534","407050",professor);
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("No Index time = " + duration + " ms");
    t_jiselect.print ();
    
	}//gen
	
    public static void main (String [] args){ 	
    	gen();
    }
	
	
}
