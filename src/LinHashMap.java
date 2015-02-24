
/************************************************************************************
 * @file LinHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an array of buckets.
 */
public class LinHashMap <K, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int    nKeys;
        K []   key;
        V []   value;
        Bucket next;
        @SuppressWarnings("unchecked")
        Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
    } // Bucket inner class

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of home buckets (a power of 2, e.g., 4)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();
        mod1   = initSize;
        mod2   = 2 * mod1;
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();

        //  T O   B E   I M P L E M E N T E D
        
        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int i = h (key);
        //  T O   B E   I M P L E M E N T E D
        Bucket gbucket=hTable.get(i);
	    while(gbucket!=null){    
        	for(int t=0;t<gbucket.nKeys;t++){
	        	if(gbucket.key[t].equals(key))
	        		return gbucket.value[t];
	        }
        	gbucket=gbucket.next;
	    }
        return null;
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        int i = h (key);
        //  T O   B E   I M P L E M E N T E D
       
        if(hTable.size()==0){
        	for(int t=0;t<mod1;t++)
        	hTable.add(new Bucket(null));
        	
        }
        if(i<split){
        i=h2(key);
        }
                
        Bucket cbucket=hTable.get(i);
        List <Bucket> fbucket= new ArrayList <> ();
        boolean spillover=false;
        while(true){
	        if(cbucket.nKeys<SLOTS){
	        	cbucket.key[cbucket.nKeys]=key;
	        	cbucket.value[cbucket.nKeys]=value;
	        	cbucket.nKeys=cbucket.nKeys+1;
	        	fbucket.add(cbucket);	        		
	        	break;
		    }
	        else{
	        	spillover=true;
	        	fbucket.add(cbucket);
	        	if(cbucket.next!=null){
	        		cbucket=cbucket.next;
	        	}
	        	else{
	        		cbucket.next=new Bucket(null);
	        		cbucket=cbucket.next;
	        	}
	        }
        }

        for(int t=0;t<fbucket.size()-1;t++){
        	Bucket tbucket=fbucket.get(t);
        	tbucket.next=fbucket.get(t+1);
        	fbucket.set(t, tbucket);
        }

        Bucket ffbucket=fbucket.get(0);
        hTable.set(i, ffbucket);
        if(spillover){
        	if(split==0){
            	for(int t=0;t<mod1;t++)
            	hTable.add(new Bucket(null));
            	
            }
        	
            Bucket spillbucket=hTable.get(split);
//            Bucket oldbucket=new Bucket(null);
//            Bucket newbucket=new Bucket(null);
            int oldb=0;
            int newb=0;
            List <K> oldbucketK=new ArrayList <> ();
            List <V> oldbucketV=new ArrayList <> ();
            List <K> newbucketK=new ArrayList <> ();
            List <V> newbucketV=new ArrayList <> ();
            while(spillbucket!=null){
    	        for(int t=0;t<spillbucket.nKeys;t++){
    	        	if(h2(spillbucket.key[t])==split){
//    	        		oldbucket.key[oldb]=spillbucket.key[t];
//    	        		oldbucket.value[oldb]=spillbucket.value[t];
//    	        		oldbucket.nKeys=oldbucket.nKeys+1;
    	        		oldbucketK.add(spillbucket.key[t]);
    	        		oldbucketV.add(spillbucket.value[t]);
    	        		oldb=oldb+1;
    	        	}
    	        	else{
//    	        		newbucket.key[newb]=spillbucket.key[t];
//    	        		newbucket.value[newb]=spillbucket.value[t];
//    	        		newbucket.nKeys=newbucket.nKeys+1;
    	        		newbucketK.add(spillbucket.key[t]);
    	        		newbucketV.add(spillbucket.value[t]);
    	        		newb=newb+1;
    	        	}
    	        }
    	        spillbucket=spillbucket.next;
            }
            List <Bucket> oldbucketL= new ArrayList <> ();
            List <Bucket> newbucketL= new ArrayList <> ();
            oldb=0;
            newb=0;
            while(true){
            	Bucket temp=new Bucket(null);
            	for(int t=0;t<SLOTS;t++){
            		if(oldb>oldbucketV.size()-1){
            			break;
            		}
            		temp.key[t]=oldbucketK.get(oldb);
            		temp.value[t]=oldbucketV.get(oldb);
            		temp.nKeys=t+1;
            		oldb=oldb+1;
            	}
            	oldbucketL.add(temp);
            	if(oldb>oldbucketK.size()-1){
            		oldbucketL.add(null);
            		break;
            	}
            }
            while(true){
            	Bucket temp=new Bucket(null);
            	for(int t=0;t<SLOTS;t++){
            		if(newb>newbucketK.size()-1){
            			break;
            		}
            		temp.key[t]=newbucketK.get(newb);
            		temp.value[t]=newbucketV.get(newb);
            		temp.nKeys=t+1;
            		newb=newb+1;
            	}
            	newbucketL.add(temp);
            	if(newb>newbucketK.size()-1){
            		newbucketL.add(null);
            		break;
            	}
            }
            fbucket= new ArrayList <> ();
            for(int t=0;t<oldbucketL.size()-1;t++){
            	Bucket tbucket=oldbucketL.get(t);
            	tbucket.next=oldbucketL.get(t+1);
            	oldbucketL.set(t, tbucket);
            }
            if(oldbucketL.size()==0){
            	ffbucket=new Bucket(null);
            }
            else{
             ffbucket=oldbucketL.get(0);
            }
            hTable.set(split, ffbucket);
            fbucket= new ArrayList <> ();
            for(int t=0;t<newbucketL.size()-1;t++){
            	Bucket tbucket=newbucketL.get(t);
            	tbucket.next=newbucketL.get(t+1);
            	newbucketL.set(t, tbucket);
            }
            if(newbucketL.size()==0){
            	ffbucket=new Bucket(null);
            }
            else{
             ffbucket=newbucketL.get(0);
            }
            hTable.set(split+mod1, ffbucket);
            if(split==mod1-1){
            	mod1=mod2;
            	mod2=2*mod1;
            	split=0;
            }
            else{
            split=split+1;
            }
        	
        }
        return null;
    } // put

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table. 
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * (mod1 + split);
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Linear Hashing)");
        out.println ("-------------------------------------------");

        //  T O   B E   I M P L E M E N T E D
        for(int i=0;i<hTable.size();i++){
        	out.print("Bucket"+i);
        	Bucket printbucket=hTable.get(i);
        	while(printbucket!=null){
	        	for(int t=0;t<printbucket.nKeys;t++){
	        		out.print("  "+printbucket.value[t]+"   ");
	        	}
	        	printbucket=printbucket.next;
        	}
        	out.println();
        }
        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key)
    {
        return key.hashCode () % mod1;
    } // h

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key)
    {
        return key.hashCode () % mod2;
    } // h2

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class, 11);
        int nKeys = 20000;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        //for (int i = 1; i < nKeys; i += 2) ht.put (i, i * i);
        for(int i=0;i<89;i++){
        	ht.put(i, i);
        }
       
        ht.print ();
        for (int i = 0; i < 1; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
//        out.println ("key = " + "36,dsf,dsf" + " value = " + ht.get ("36,dsf,dsf"));
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
       
        
    } // main

} // LinHashMap class

