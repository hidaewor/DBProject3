/*****************************************************************************************
 * @file  Tester.java
 * @author Hope Idaewor
 */

import static java.lang.System.out;

import java.util.Arrays;
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
    
    test.addRelSchema ("Course",
			            "crsCode deptId crsName descr",
			            "String String String String",
			            "crsCode",
			            null);

    test.addRelSchema ("Teaching",
			            "crsCode semester profId",
			            "String String Integer",
			            "crcCode semester",
			            new String [][] {{ "profId", "Professor", "id" },
			                             { "crsCode", "Course", "crsCode" }});

    test.addRelSchema ("Transcript",
			            "studId crsCode semester grade",
			            "Integer String String String",
			            "studId",
			            new String [][] {{ "studId", "Student", "id"},
			                             { "crsCode", "Course", "crsCode" },
			                             { "crsCode semester", "Teaching", "crsCode semester" }});

    /* Create Tables */
    Table student = new Table("student", "id name address status", "Integer String String String", "id");
    Table professor = new Table("professor","id name deptId", "Integer String String","id");
    Table transcript = new Table("transcript","studId crsCode semester grade", "Integer String String String","studId");
    
    String [] tables = { "Student", "Professor", "Course", "Teaching", "Transcript" };
    
    /* Insert Tuples */
    int ntups [] = new int [] {5000,5000,1000,1000,1000}; //IN ORDER
    Comparable[][][] tups = test.generate(ntups);
    
    out.println("DDL> Inserting 5000 Students, 2000 Professors, 3000 Transcripts..");
    for (int i = 0; i <tups.length; i++) {
        for (int j = 0; j < tups[i].length; j++) {
        	
        	if (tables[i].equals("Student")){
                	student.insert(tups[i][j]);
                }
        	else if(tables[i].equals("Professor")){
                	professor.insert(tups[i][j]);
                }
	        else if(tables[i].equals("Transcript")){
	        		//out.println(Arrays.toString(tups[i][j]));
	        		transcript.insert(tups[i][j]);
	        }
        } // for
    } // for    
    
    //Print tables
   //student.print();
   //professor.print();
    //transcript.print();
    //student.printIndex();


    /* Case 1: Select Point Query = COMPLETE*/
    
    //--------------------- no index select
    out.println ();
    out.println("----Case 1.1: Select Point Query, No Index----");
    double sum = 0, avg = 0;
    for(int x = 0; x < 4; x++){
	    startTime = System.currentTimeMillis();
	    Table t_select = student.select (t -> t[student.col("status")].equals ("status202834"));
	    endTime = System.currentTimeMillis();
	    duration = (endTime - startTime); 
	    if (x!=0){ //skip compililation time
	    sum = sum + duration;
	    out.println("Time " + x + " = " + duration);
	    }
    }
    avg = sum/3;
    out.println("Avg Time = " + avg + "ms");
    //t_select.print ();
 
    //--------------------- indexed select (currently using TreeMap, change in Table.java constructors)
    out.println ();
    out.println("----Case 1.2: Select Point Query, Indexed----");
    sum = 0; avg = 0;
    for(int x = 0; x < 4; x++){
	    startTime = System.currentTimeMillis();
	    Table t_iselect = student.select (new KeyType (864198));
	    endTime = System.currentTimeMillis();
	    duration = (endTime - startTime); 
	    sum = sum + duration;
	    out.println("Time " + x + " = " + duration);
    }
    avg = sum/4;
    out.println("Avg Time = " + avg + "ms");
    //t_iselect.print ();


   /* Case 2: Select Range Query = COMPLETE
    */
    
    //--------------------- no index select
    out.println ();
    out.println("----Case 2.1: Select Range Query, No Index----");
    sum = 0; avg = 0;
    for(int x = 0; x < 4; x++){
	    startTime = System.currentTimeMillis();
	    Table t_rselect=student.select(50000, 250000);
	    endTime = System.currentTimeMillis();
	    duration = (endTime - startTime); 
	    sum = sum + duration;
	    out.println("Time " + x + " = " + duration);
    }
    avg = sum/4;
    out.println("Avg Time = " + avg + "ms");
    //t_rselect.print ();
 
   
    //--------------------- indexed select (currently using TreeMap, change in Table.java)
    out.println ();
    out.println("----Case 2.2: Select Range Query, Indexed----");
    sum = 0; avg = 0;
    for(int x = 0; x < 4; x++){
	    startTime = System.currentTimeMillis();
	    Table t_riselect = student.select (680080, 900000);
	    endTime = System.currentTimeMillis();
	    duration = (endTime - startTime); 
	    sum = sum + duration;
	    out.println("Time " + x + " = " + duration);
    }
    avg = sum/4;
    out.println("Avg Time = " + avg + "ms");
    //t_riselect.print ();
   
    
   /* Case 3: Join = COMPLETE*/

    //--------------------- no index join
    out.println ();
    out.println("----Case 3.1: Join, No Index----");
    sum = 0; avg = 0;
    for(int x = 0; x < 4; x++){
	    startTime = System.currentTimeMillis();
	    Table t_jselect = student.join("id", "studId", transcript);
	    endTime = System.currentTimeMillis();
	    duration = (endTime - startTime); 
	    sum = sum + duration;
	    out.println("Time " + x + " = " + duration);
    }
    avg = sum/4;
    out.println("Avg Time = " + avg + "ms");
    //t_jselect.print ();
    
    //--------------------- indexed join, only primary key only has index
    out.println ();
    out.println("----Case 3.2: Join, Indexed----");
    sum = 0; avg = 0;
    for(int x = 0; x < 4; x++){
	    startTime = System.currentTimeMillis();
	    Table t_jiselect = student.indexedJoin("id", "studId", transcript);
	    endTime = System.currentTimeMillis();
	    duration = (endTime - startTime); 
	    sum = sum + duration;
	    out.println("Time " + x + " = " + duration);
    }
    avg = sum/4;
    out.println("Avg Time = " + avg + "ms");
    //t_jiselect.print ();
    
    
	}//gen
	
    public static void main (String [] args){ 	
    	gen();
    }
	
	
}
