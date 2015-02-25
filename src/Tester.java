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
    int ntups [] = new int [] {10000,2}; //student = 3, professor = 3
    Comparable[][][] tups = test.generate(ntups);
    for (int i = 0; i <tups.length; i++) {
        for (int j = 0; j < tups[i].length; j++) {
        	if (tables[i].equals("Student")){
                	student.insert(tups[i][j]);
                }
            else{
                	professor.insert(tups[i][j]);
                }
        } // for
        out.println ();
    } // for    
    
    //Print tables
    student.print();
    professor.print();
    //student.printIndex();
    
    //Problems:
    //1. We need thousands of tuples, put each case in a loop to get the different times for standard deviation
    //2. Ext & Lin has map arent working in the "set method"
    //3. BpTree isnt working in split
    
    long startTime, endTime, duration;
    
    /* Case 1: Select Point Query */
    
    //--------------------- no index select
    out.println ();
    startTime = System.currentTimeMillis();
    Table t_select = student.select (t -> t[student.col("status")].equals ("status762589"));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("No Index time = " + duration + " ms");
    t_select.print ();
 
    //--------------------- indexed select (currently using TreeMap, change in Table.java)
    out.println ();
    startTime = System.currentTimeMillis();
    Table t_iselect = student.select (new KeyType (680080));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("Indexed Select time = " + duration + " ms");
    t_iselect.print ();

 
   /* Case 2: Select Range Query */
    
    //--------------------- no index select
    out.println ();
    startTime = System.currentTimeMillis();
    Table t_rselect = student.select (t -> t[student.col("status")].equals ("status762589"));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("No Index time = " + duration + " ms");
    t_rselect.print ();
    
    //--------------------- indexed select (currently using TreeMap, change in Table.java)
    out.println ();
    startTime = System.currentTimeMillis();
    Table t_riselect = student.select (new KeyType (680080));
    endTime = System.currentTimeMillis();
    duration = (endTime - startTime); 
    out.println("Indexed Select time = " + duration + " ms");
    t_riselect.print ();
     
  
   /* Case 3: Join */

    
    
	}//gen
	
    public static void main (String [] args){ 	
    	gen();
    }
	
	
}
