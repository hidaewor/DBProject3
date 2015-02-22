
/************************************************************************************
 * @file ExtHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;

import static java.lang.System.out;

import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Extendable Hashing algorithm.  Buckets
 * are allocated and stored in a hash table and are referenced using directory dir.
 */
public class ExtHashMap <K, V>
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
        int  nKeys;
        int localDepth;
        int bNumber;
        K [] key;
        V [] value;
        @SuppressWarnings("unchecked")
        Bucket ()
        {
        	localDepth = 0;
            nKeys = 0;
            //we initialize with 1 more slot in order to make it easier to split
            //if array is ever full, split it
            key   = (K []) Array.newInstance (classK, SLOTS+1);
            value = (V []) Array.newInstance (classV, SLOTS+1);
        } // constructor
        
        //method for cloning buckets. Used in set 
        public void setEqual(Bucket aBucket){
        	key   = (K []) Array.newInstance (classK, SLOTS+1);
            value = (V []) Array.newInstance (classV, SLOTS+1);
            
        	nKeys = aBucket.nKeys;
        	localDepth = aBucket.localDepth;
        	for(int x = 0;x<aBucket.nKeys;x++){
        		key[x] = aBucket.key[x];
        		value[x] = aBucket.value[x];
        	}
        }
    } // Bucket inner class

    /** The hash table storing the buckets (buckets in physical order)
     */
    private final List <Bucket> hTable;

    /** The directory providing access paths to the buckets (buckets in logical oder)
     */
    private final List <Bucket> dir;

    /** The modulus for hashing (= 2^D) where D is the global depth
     */
    private int mod;
    
    /**
     * The global depth of the Hash Map
     */
    private int globalDepth;

    /** The number of buckets
     */
    private int nBuckets;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;
    public int currentB = 1;

    /********************************************************************************
     * Construct a hash table that uses Extendable Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of buckets (a power of 2, e.g., 4)
     */
    public ExtHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();   // for bucket storage
        dir    = new ArrayList <> ();   // for bucket access
        mod    = nBuckets = initSize;
        
        //calculate global depth
        int i = nBuckets;
        while(i >1){
        	i = i/2;
        	globalDepth+=1;
        }
        //System.out.println("depth: "+globalDepth);
        //I don't see why we need an htable. Fill dir with buckets
        for(int j = 0; j<nBuckets;j++){
        	Bucket b = new Bucket();
        	b.bNumber = currentB;
        	currentB +=1;
        	b.localDepth = globalDepth;
        	dir.add(b);
        	hTable.add(b);
        }
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();

        //  T O   B E   I M P L E M E N T E D
        List<K> a=new ArrayList();
        List<V> b=new ArrayList();
        for(int i=0;i<hTable.size();i++){
        	for(int j=0;j<hTable.get(i).nKeys;j++){
        		a.add(hTable.get(i).key[j]);
        		b.add(hTable.get(i).value[j]);
        	}
        }
        Map<K,V> m=new HashMap<K,V>();
        for(int i=0;i<a.size();i++){
        	m.put(a.get(i), b.get(i));
        }
        enSet.add((java.util.Map.Entry<K, V>) m);
            
        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int    i = h (key);
        Bucket b = dir.get (i);
        //If the directory we get is empty, we want to use a weaker mod until we find a full one
        int origMod = mod; 
        while(b.nKeys == 0 && mod != 1){
        	mod = mod/2;
        	//System.out.println(""+mod + " " + b.nKeys);
        	b = dir.get(h(key));
        }
        mod = origMod;
        //  T O   B E   I M P L E M E N T E D
        for(int t=0;t<b.nKeys;t++){
        	if(b.key[t].equals(key))
        		return b.value[t];
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
    	//System.out.println("adding " +key + " to " + b.nKeys);
        int    i = h (key);
        Bucket b = dir.get (i);
        //System.out.println("adding " +key + " to " + b.nKeys);
        //  T O   B E   I M P L E M E N T E D
        b.key[b.nKeys] = key;
        b.value[b.nKeys] = value;
        b.nKeys +=1;
        //if after insertion, nkeys is slots+1, we need to split the bucket
        //note that nkeys should never be greater than, but we'll check just in case
        if(b.nKeys >= SLOTS +1){
        	splitBucket(b, i);
        }
        //System.out.println(""+mod);
        
        return null;
    } // put
    
    //where I is the computed i in put
    public void splitBucket(Bucket b, int i){
    	//System.out.println("here");
    	//Split the keys between new buckets
    	//if the global depth == the localdepth, we have to double the size
    	//System.out.println("" + globalDepth + "  " + b.localDepth);
    	if(globalDepth <= b.localDepth){
    		//System.out.println("here2");
    		globalDepth +=1;
    		b.localDepth +=1;
    		for(int startingPoint = mod;startingPoint<mod*2;startingPoint++){
            	dir.add(new Bucket());
    			//dir.add(null);
            	if(h(startingPoint) != i){
            		dir.set(startingPoint, dir.get(h(startingPoint)));
            		//System.out.println("here3");
            	}
            	//Otherwise this is the bucket we want to split with
            	else{
            		mod = mod*2;
            		//System.out.println("here4");
            		Bucket b2 = new Bucket();
                	Bucket newB = new Bucket();
                	newB.localDepth = b.localDepth;
                	b2.localDepth = globalDepth;
                	b2.nKeys = 0;
                	newB.nKeys = 0;
                	for(int num = 0;num <b.nKeys;num++){
                		if(h(b.key[num])>=mod/2){
                			b2.key[b2.nKeys]=b.key[num];
                			b2.value[b2.nKeys]=b.value[num];
                			b2.nKeys +=1;
                			//System.out.println("here5");
                		}
                		else{
                			//System.out.println("here7");
                			newB.key[newB.nKeys]=b.key[num];
                			newB.value[newB.nKeys]=b.value[num];
                			newB.nKeys +=1;
                		}
                	}
                	//make b == newB
                	b.setEqual(newB);
                	dir.set(i+mod/2, b2);
                	hTable.add(b2);
                	mod = mod/2;
                	b2.bNumber = currentB;
                	currentB +=1;
            	}
            	
    		}
    		mod = mod*2;
    		
    	}
    	
    	else{
    		//System.out.println("here6");
    		b.localDepth +=1;
    		//split the bucket without creating the rest of the directory
    		Bucket b2 = new Bucket();
        	Bucket newB = new Bucket();
        	newB.localDepth = b.localDepth;
        	b2.localDepth = b.localDepth;
        	b2.nKeys = 0;
        	newB.nKeys = 0;
        	b2.bNumber = currentB;
        	currentB +=1;
        	for(int num = 0;num <b.nKeys;num++){
        		if(h(b.key[num])>=mod/2){
        			//System.out.println("here8");
        			b2.key[b2.nKeys]=b.key[num];
        			b2.value[b2.nKeys]=b.value[num];
        			b2.nKeys +=1;
        		}
        		else{
        			//System.out.println("here9");
        			newB.key[newB.nKeys]=b.key[num];
        			newB.value[newB.nKeys]=b.value[num];
        			newB.nKeys +=1;
        		}
        	}
        	b.setEqual(newB);
        	//System.out.println("currentMod = "+ mod);
        	//System.out.println("i is = "+ i);
        	//System.out.println("dir index is = "+ (i+mod/2));
        	//In this case, we dont need to do i+mod/2, because mod
        	//has already been increased
        	//dir.set(i, b2);
        	//update directory
        	if(b2.nKeys>0){
        		int factor = h(b2.key[0]);
        		while(factor<dir.size()){
        			dir.set(factor, b2);
        			factor +=mod;
        		}
        	}
        	if(b.nKeys>0){
        		int factor = h(b.key[0]);
        		while(factor<dir.size()){
        			dir.set(factor, b);
        			factor +=mod;
        		}
        	}
        	hTable.add(b2);
        	if(b.nKeys == 5){
        		splitBucket(b, i);
        	}
        	if(b2.nKeys ==5){
        		splitBucket(b2, i);
        	}
    	}
    	nBuckets = hTable.size();
    }

    /********************************************************************************
     * Return the size (SLOTS * number of buckets) of the hash table. 
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * nBuckets;
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Extendable Hashing)");
        out.println ("-------------------------------------------");

        //  T O   B E   I M P L E M E N T E D
        for(int i=0;i<hTable.size();i++){
        	Bucket b=hTable.get(i);
        	out.print("Bucket"+b.bNumber);
	        for(int t=0;t<b.nKeys;t++){
	        	out.print("  "+b.value[t]+"   ");
	        }
        	out.println();
        }
        out.println("Directory: ");
        for(int i=0;i<dir.size();i++){
        	
        	Bucket b=dir.get(i);
        	out.print("Dir "+ i + " points to Bucket "+b.bNumber);
        	out.println();
        }
        //Also print out the Directory
        
        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the hash function.
     * @param key  the key to hash
     * @return  the location of the directory entry referencing the bucket
     */
    private int h (Object key)
    {
        return key.hashCode () % mod;
    } // h

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
    	//This HAS to be a power of 2. That's how ExtHashMaps work..
        ExtHashMap <Integer, Integer> ht = new ExtHashMap <> (Integer.class, Integer.class, 2);
        int nKeys = 35;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 1) ht.put (i, i * i);
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // ExtHashMap class