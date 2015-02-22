
/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;

import static java.lang.System.out;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;



/************************************************************************************
 * This class provides B+Tree maps.  B+Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 */
public class BpTreeMap <K extends Comparable <K>, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The maximum fanout for a B+Tree node.
     */
    private static final int ORDER = 5;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;
        int       nKeys;
        K []      key;
        Object [] ref;
        
        //added these to make working with B+ trees easier
        Node parentNode=null;
        Node rightNode=null; //can be child or sibling nodes
        Node leftNode=null;
      
        
        @SuppressWarnings("unchecked")
        Node (boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, ORDER - 1);
            if (isLeaf) {
                //ref = (V []) Array.newInstance (classV, ORDER);
                ref = new Object [ORDER];
            } else {
                ref = (Node []) Array.newInstance (Node.class, ORDER);
            } // if
        } // constructor
    } // Node inner class

    /** The root of the B+Tree
     */
    private Node root;	//removed final modifier from the original skeleton

    /** The counter for the number nodes accessed (for performance testing).
     */
    private int count = 0;

    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        root   = new Node (true);
    } // constructor

    /********************************************************************************
     * Return null to use the natural order based on the key type.  This requires the
     * key type to implement Comparable.
     */
    public Comparator <? super K> comparator () 
    {
        return null;
    } // comparator

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();
        
       Node currentPos=root; //our current position starts at the top of the tree
       
       while(currentPos.isLeaf==false){ //if not the leaf, continue to first(leftmost) leaf
    	   currentPos=(BpTreeMap<K, V>.Node) currentPos.ref[0];
       }
       
      while(currentPos!=null){//adds all the elements of the current node and continues to the right until it is null
       for(int i=0;i<currentPos.nKeys;i++){
        enSet.add(new SimpleEntry(currentPos.key[i], currentPos.ref[i]));
       }
       currentPos=currentPos.rightNode;
      }
            
        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /********************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        insert (key, value, root, null);
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */
    public K firstKey () 
    {
    	Node mostLeftNode = root;
    	//if(count == 0){//no keys have been accessed
			//throw new NoSuchElementException();}
    	
		while (mostLeftNode.isLeaf==false){
			mostLeftNode = (Node) mostLeftNode.ref[0];
		}
		return mostLeftNode.key[0];
    } // firstKey

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey () 
    {
    	//Implemented
    	Node largest = root;
    	
    	//if(count == 0)//no keys have been accessed
			//throw new NoSuchElementException();

		while (!largest.isLeaf){
			largest = (Node) largest.ref[largest.nKeys];
		}
		
		return largest.key[largest.nKeys - 1];
    } // lastKey

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {
    	Node currentNode = root;//start at the top node
		while (!currentNode.isLeaf){//while it is not a leaf
			boolean gotIt=false;
			for (int i = 0; i < currentNode.nKeys; i++){
				if (toKey.compareTo(currentNode.key[i]) >= 0){
					currentNode = (Node)currentNode.ref[i + 1];
					gotIt=true;
					break;
				}
			}
			if (gotIt==false){
				currentNode = (Node)currentNode.ref[0];
			}
		}

		SortedMap<K, V> map=new TreeMap<>();//creates a map
		for (int i = 0; i < currentNode.nKeys; i++){
			if (currentNode.key[i].compareTo(toKey) < 0){
				map.put(currentNode.key[i], (V)currentNode.ref[i]);
				continue;
			}
			break;
		}
		
		currentNode=currentNode.leftNode;
		
		while(currentNode != null){
			for (int i=0; i<currentNode.nKeys; i++){
				map.put(currentNode.key[i], (V)currentNode.ref[i]);
			}
			
			currentNode=currentNode.leftNode;
		}
    	

        return map;
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
    	Node currentNode=root;//current location
		while (!currentNode.isLeaf){//while it is not the leaf
			boolean gotIt=false;
			for (int i=0; i<currentNode.nKeys; i++){
				if (fromKey.compareTo(currentNode.key[i]) >= 0){
					currentNode=(Node)currentNode.ref[i + 1];
					gotIt= true;
					break;
				}
			}
			if (!gotIt){
				currentNode=(Node)currentNode.ref[0];
			}
		}

		SortedMap<K, V> map=new TreeMap<>();
		for (int i=0; i<currentNode.nKeys; i++){
			if (currentNode.key[i].compareTo(fromKey) >= 0){
				map.put(currentNode.key[i], (V)currentNode.ref[i]);
				continue;
			}
		}
		
		currentNode=currentNode.rightNode;
		
		while(currentNode!= null){
			for (int i=0; i<currentNode.nKeys; i++){
				map.put(currentNode.key[i], (V)currentNode.ref[i]);
			}
			currentNode=currentNode.rightNode;
		}

		return map;
		
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {
    	
    	SortedMap hMap = headMap(toKey);
		SortedMap tMap = tailMap(fromKey);
		
		hMap.keySet().retainAll(tMap.keySet());
		
		
		return hMap;

        
    } // subMap

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
        int sum = 0;

      
		Node currentNode=root;
		while (!currentNode.isLeaf){//while it is not a leaf
			currentNode=(Node) currentNode.ref[0];//keep going all the way to the bottom left
		}
		
		while(currentNode!=null){
			sum=sum+currentNode.nKeys;
			currentNode=currentNode.rightNode;
		}

        return  sum;
    } // size

    /********************************************************************************
     * Print the B+Tree using a pre-order traveral and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
        } // if

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param ney  the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key.compareTo (k_i) <= 0) {
                if (n.isLeaf) {
                    return (key.equals (k_i)) ? (V) n.ref [i] : null;
                } else {
                    return find (key, (Node) n.ref [i]);
                } // if
            } // if
        } // for
        return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]); 
        
    } // find

    /********************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param p    the parent node
     */
    private void insert (K key, V ref, Node n, Node p)
    {   
    	if(n.isLeaf==false){//if false, navigate to correct position
    		
    		while(n.isLeaf==false){//keep searching until we find the correct leaf
    			
    			for(int i=0;i<n.nKeys;i++){
    				if(key.compareTo(n.key[i])<0){//if less than, then go down
    					n=(BpTreeMap<K, V>.Node) n.ref[i];
    					break;
    				}
    				else if(i==n.nKeys-1){//if at the end of key, pick rightmost branch
    					n=(BpTreeMap<K, V>.Node) n.ref[i+1];    					
    					
    					//out.println("navigated here");
    					break;
    				}
    			}   			
    		}
    	}
    	
    	boolean beenInserted=false;//tracks if the key has been inserted or not. w/o this tracker, the original skeleton code will create an out-of-bounds error for unsorted insertions
        if (n.nKeys < ORDER - 1) {	//if not full
            for (int i = 0; i < n.nKeys; i++) {
                K k_i = n.key [i];
                //out.println("loop wedge key= "+key);
                if (key.compareTo (k_i) < 0) {
                	//out.println("i= "+i);
                    wedge (key, ref, n, i);
                    beenInserted=true;
                    break;
                } else if (key.equals (k_i)) {
                    out.println ("BpTreeMap:insert: attempt to insert duplicate key = " + key);
                } // if
                
            } // for   
            
          if(beenInserted==false){  wedge (key, ref, n, n.nKeys);}
           // out.println("key2= "+key);
        } else {
        	
            Node sib = split (key, ref, n);//I did most of the stuff inside the split node

        } // if
        
        
        /*
        if((int)key==9){
        	
        	Node pos=root;
        	while(pos.isLeaf==false){
    			pos=(BpTreeMap<K, V>.Node) pos.ref[0];
        	}
        	
        	while(pos!=null){
        	for(int i=0;i<pos.nKeys;i++){
        		out.println("key: "+pos.key[i]+" value: "+pos.ref[i]);
        	}
        	pos=pos.rightNode;
        	}
        }   
        */
        
        
    } // insert

    /********************************************************************************
     * Wedge the key-ref pair into node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     */
    private void wedge (K key, V ref, Node n, int i)
    {
        for (int j = n.nKeys; j > i; j--) {
            n.key [j] = n.key [j - 1];
            n.ref [j] = n.ref [j - 1];
        } // for
        n.key [i] = key;
        n.ref [i] = ref;
        n.nKeys++; 
        
    } // wedge
     
    
    public class tuples {//makes life easier by pairing up the key and ref for sorting purposes
		public K keyT;
		public Object refT;
		
		public tuples(K key1, Object ref1){
			keyT = key1;
			refT = ref1;
		}
	}

    /********************************************************************************
     * Split node n and return the newly created node.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     */
    private Node split (K key, V ref, Node n)
    {
    	//out.println();
       // out.println ("split at key= "+key);
    	
    	Node returnedNode;
        
        List<tuples> tuples01=new ArrayList<>();//puts the tuples into a sorted list for easy handling
        for(int i=0;i<n.nKeys;i++){
        	tuples01.add(new tuples(n.key[i],n.ref[i]));
        }
        tuples01.add(new tuples(key, ref));
        
        
        Comparator<tuples> tupleOrdering = new Comparator<BpTreeMap<K,V>.tuples>() {
			public int compare(tuples t1, tuples t2) {
				return t1.keyT.compareTo(t2.keyT);
			}
		};
        Collections.sort(tuples01, tupleOrdering);//put them in order based on keys
        
        Node newNode=new Node(true);
        if(n.isLeaf==false){//if the node we are splitting is not a leaf node, then its sibling node is not a leaf node also. 
    	   newNode.isLeaf=false;
    	   }
        
        for(int i=0;i<tuples01.size()-ORDER/2;i++){
        	if(tuples01.get(ORDER/2+i).keyT!=null){

        	newNode.key[i]=tuples01.get(ORDER/2+i).keyT;
        	newNode.ref[i]=tuples01.get(ORDER/2+i).refT;
        	newNode.nKeys++;        		
        	}
        }
        
        n.rightNode=newNode;
        
        for(int i=n.key.length-1;i>n.key.length/2-1;i--){//updates current node: n
        	n.key[i]=null;
        	n.ref[i+1]=null;
        	n.nKeys--;
        }
        
        if(n.parentNode==null){//creates a parent node if there is none
        	//if((int)key==19){out.println("key=19 too"); }
        	Node newParent=new Node(false);
        	newParent.key[0]=tuples01.get(ORDER/2).keyT;//stores key ("smallest right" key of the right sibling node)
        	newParent.nKeys++;
        	newParent.ref[0]=n;//left child is node n
        	newParent.ref[1]=newNode;//right child is new node
        	root=newParent;
        	n.parentNode=newParent;
        	n.rightNode.parentNode=newParent;
        	
        	//out.println("created new parent node @ key= "+key);  
        	//out.println("newParent.key= "+newParent.key[0]); 
        	//Node testNode=(BpTreeMap<K, V>.Node) newParent.ref[1];
        	//out.println("testNode.ref0= "+testNode.ref[0]);  
        }
        else{//else store the "smallest right" key into the parent node
        	if(n.parentNode.nKeys!=ORDER-1){//if the parent node of n is not full   
        		for(int i=0;i<n.parentNode.nKeys;i++){//finds the correct position and then shift everything to the right before adding
        			
        				if(tuples01.get(ORDER/2).keyT.compareTo(n.parentNode.key[i])<0){//@correct position				        					
        			        for(int i2=n.parentNode.key.length-1;i2>i;i2--){
        			        
            			        n.parentNode.key[i2]=n.parentNode.key[i2-1];//shift key to the right
            			        n.parentNode.ref[i2+1]=n.ref[i2];//shift ref(link to leaf nodes) to the right
        			        }  
        			        n.parentNode.key[i]=tuples01.get(ORDER/2).keyT;
        			        n.parentNode.nKeys++;
        			        n.parentNode.ref[i+1]=n.rightNode;    
        			        
        			        
        			        n.rightNode.parentNode=n.parentNode;
        					break;
        				}
        				else if(i==n.parentNode.nKeys-1){//if it is not less than any of the keys, then it is the greatest key
        					n.parentNode.key[i+1]=tuples01.get(ORDER/2).keyT;
        					n.parentNode.nKeys++;
        					n.parentNode.ref[i+2]=n.rightNode;
        						
        					 n.rightNode.parentNode=n.parentNode;
        					
        					break;
        				}
        		}
        	}
        	else{//else it is full, so split parent node also
        		//out.println("parentNode is full @key="+key);
        		
        		for(int i=0;i<ORDER-1;i++){
        			//out.println(n.key[i]);
        			//out.println(n.rightNode.key[i]);
        			//out.println(n.parentNode.key[i]);
        		}
 
        		K dividerKey=tuples01.get(ORDER/2).keyT;
        		V dividerRef=(V) tuples01.get(ORDER/2).refT;
        		
        		//out.println("tuples.key="+dividerKey+" tuples.ref="+dividerRef);
        		Node newNode2=split(dividerKey, dividerRef, n.parentNode);   
        		n.rightNode.parentNode=newNode2;      	
        		
        		for(int i=0;i<ORDER;i++){
        			if((int)newNode.key[0]<=(int)newNode2.key[i]){        		
        				newNode2.ref[i+1]=newNode;
        				break;
        			}
        		}
        		
        	}	
        }
        
       
        return newNode;
    } // split
    
    
    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        int totKeys = 50;
        if (args.length == 1) totKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < totKeys; i += 2) {
        	bpt.put (i, i * i);
        }
        
        /*//the original skeleton code's print, get and find methods are not sufficient or buggy and will cause errors if you go beyond 10 or 20ish elements. Since it is not our jobs to implement/fix them. I will leave them as they are.
        bpt.print (bpt.root, 0);
        for (int i = 0; i < totKeys; i++) {
            out.println ("key = " + i + " value = " + bpt.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totKeys);
        */
        System.out.println();
        System.out.println("The hashset of keys and values:");
        System.out.println("(key=value)");
        HashSet enSet=(HashSet) bpt.entrySet();//this will return the correct keys and values at the leaf nodes unlike the original skeleton code's get/find methods.
		Iterator it=enSet.iterator();
        while(it.hasNext())
        {
          System.out.println(it.next());
        }
        
        System.out.println();

		System.out.println("First Key: " + bpt.firstKey());
		System.out.println("Last Key: " + bpt.lastKey());
		System.out.println();
		out.println("Head map for 3");
		SortedMap hMap = bpt.headMap(new Integer(3));
		for (Object obj : hMap.entrySet()){
			System.out.println(obj);
		}
		System.out.println();
		out.println("Tail map for 7");
		SortedMap tMap = bpt.tailMap(new Integer(7));
		for (Object obj : tMap.entrySet()){
			System.out.println(obj);
		}
		
		System.out.println();
		out.println("Sub map for 3 to 7");
		SortedMap sMap= bpt.subMap(new Integer(3), new Integer(7));
		for (Object obj : sMap.entrySet()){
			System.out.println(obj);
		}
		System.out.println();
		out.println("Size: " + bpt.size());
		
        
        
        
        
    } // main

} // BpTreeMap class