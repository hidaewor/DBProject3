
/*******************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import static java.lang.System.out;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.TreeMap;

/*******************************************************************************
 * This class provides B+Tree maps. B+Trees are used as multi-level index
 * structures that provide efficient access for both point queries and range
 * queries.
 */
@SuppressWarnings("all")
public class BpTreeMap<K extends Comparable<K>, V> extends AbstractMap<K, V>
		implements Serializable, Cloneable, SortedMap<K, V> {
	/**
	 * The maximum fanout for a B+Tree node.
	 */
	private static final int ORDER = 5;

	/**
	 * The class for type K.
	 */
	private final Class<K> classK;

	/**
	 * The class for type V.
	 */
	private final Class<V> classV;

	/***************************************************************************
	 * This inner class defines nodes that are stored in the B+tree map.
	 * 
	 * 
	 */
	private class Node {
		boolean isLeaf;
		int nKeys;
		K[] key;
		Object[] ref;
		
		Node parent;
		Node left = null;
		Node right = null;

		@SuppressWarnings("unchecked")
		Node(boolean _isLeaf) {
			isLeaf = _isLeaf;
			nKeys = 0;
			key = (K[]) Array.newInstance(classK, ORDER - 1);
			if (isLeaf) {
				// ref = (V []) Array.newInstance (classV, ORDER);
				ref = new Object[ORDER];
			} else {
				ref = (Node[]) Array.newInstance(Node.class, ORDER);
			} // if
		} // constructor
	} // Node inner class

	/**
	 * The root of the B+Tree
	 */
	private Node root;

	/**
	 * The counter for the number nodes accessed (for performance testing).
	 */
	private int count = 0;

	/***************************************************************************
	 * Construct an empty B+Tree map.
	 * 
	 * @param _classK
	 *            the class for keys (K)
	 * @param _classV
	 *            the class for values (V)
	 */
	public BpTreeMap(Class<K> _classK, Class<V> _classV) {
		classK = _classK;
		classV = _classV;
		root = new Node(true);
	} // BpTree

	/***************************************************************************
	 * Return null to use the natural order based on the key type. This requires
	 * the key type to implement Comparable.
	 */
	public Comparator<? super K> comparator() {
		return null;
	} // comparator

	/***************************************************************************
	 * Return a set containing all the entries as pairs of keys and values.
	 * 
	 * @return the set view of the map
	 * 
	 * 
	 */
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> enSet = new HashSet<>();

		Node smallest = root;
		while (!smallest.isLeaf){
			smallest = (Node) smallest.ref[0];
		}

		while (smallest != null){
			for (int i = 0; i < smallest.nKeys; i++){
				enSet.add(new SimpleEntry(smallest.key[i], smallest.ref[i]));
			}
			smallest = smallest.right;
		}
		return enSet;
	} // entrySet

	/***************************************************************************
	 * Given the key, look up the value in the B+Tree map.
	 * 
	 * @param key
	 *            the key used for look up
	 * @return the value associated with the key
	 * 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		return find((K) key, root);
	} // get

	/***************************************************************************
	 * Put the key-value pair in the B+Tree map.
	 * 
	 * @param key
	 *            the key to insert
	 * @param value
	 *            the value to insert
	 * @return null (not the previous value)
	 * 
	 * 
	 */
	public V put(K key, V value) {
		insert(key, value, root, null);
		return null;
	} // put

	/***************************************************************************
	 * Return the first (smallest) key in the B+Tree map.
	 * 
	 * @return the first key in the B+Tree map.
	 * 
	 * 
	 */
	public K firstKey() {
		Node smallest = root;
		while (!smallest.isLeaf){
			smallest = (Node) smallest.ref[0];
		}
		
		return smallest.key[0];
	} // firstKey

	/***************************************************************************
	 * Return the last (largest) key in the B+Tree map.
	 * 
	 * @return the last key in the B+Tree map.
	 * 
	 * 
	 */
	public K lastKey() {
		Node biggest = root;
		while (!biggest.isLeaf){
			biggest = (Node) biggest.ref[biggest.nKeys];
		}
		
		return biggest.key[biggest.nKeys - 1];
	} // lastKey

	/***************************************************************************
	 * Return the portion of the B+Tree map where key < toKey.
	 * 
	 * @return the submap with keys in the range [firstKey, toKey)
	 * 
	 * 
	 */
	public SortedMap<K, V> headMap(K toKey) {

		Node current = root;
		while (!current.isLeaf){
			boolean found = false;
			for (int i = 0; i < current.nKeys; i++){
				if (toKey.compareTo(current.key[i]) >= 0){
					current = (Node) current.ref[i + 1];
					found = true;
					break;
				}
			}
			if (!found){
				current = (Node) current.ref[0];
			}
		}

		SortedMap<K, V> results = new TreeMap<>();
		for (int i = 0; i < current.nKeys; i++){
			if (current.key[i].compareTo(toKey) < 0){
				results.put(current.key[i], (V) current.ref[i]);
				continue;
			}
			break;
		}
		
		current = current.left;
		
		while(current != null){
			for (int i = 0; i < current.nKeys; i++){
				results.put(current.key[i], (V) current.ref[i]);
			}
			
			current = current.left;
		}
		
		return results;
	} // headMap

	/***************************************************************************
	 * Return the portion of the B+Tree map where fromKey <= key.
	 * 
	 * @return the submap with keys in the range [fromKey, lastKey]
	 * 
	 * 
	 */
	public SortedMap<K, V> tailMap(K fromKey) {
		Node current = root;
		while (!current.isLeaf){
			boolean found = false;
			for (int i = 0; i < current.nKeys; i++){
				if (fromKey.compareTo(current.key[i]) >= 0){
					current = (Node) current.ref[i + 1];
					found = true;
					break;
				}
			}
			if (!found){
				current = (Node) current.ref[0];
			}
		}

		SortedMap<K, V> results = new TreeMap<>();
		for (int i = 0; i < current.nKeys; i++){
			if (current.key[i].compareTo(fromKey) >= 0){
				results.put(current.key[i], (V) current.ref[i]);
				continue;
			}
		}
		
		current = current.right;
		
		while(current != null){
			for (int i = 0; i < current.nKeys; i++){
				results.put(current.key[i], (V) current.ref[i]);
			}
			
			current = current.right;
		}
		
		return results;
	} // tailMap

	/***************************************************************************
	 * Return the portion of the B+Tree map whose keys are between fromKey and
	 * toKey, i.e., fromKey <= key < toKey.
	 * 
	 * @return the submap with keys in the range [fromKey, toKey)
	 * 
	 * 
	 */
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		SortedMap head = headMap(toKey);
		SortedMap tail = tailMap(fromKey);
		head.keySet().retainAll(tail.keySet());
		return head;
	} // subMap

	/***************************************************************************
	 * Return the size (number of keys) in the B+Tree.
	 * 
	 * @return the size of the B+Tree
	 * 
	 *  
	 */
	public int size() {
		int sum = 0;

		Node smallest = root;
		while (!smallest.isLeaf){
			smallest = (Node) smallest.ref[0];
		}
		
		while(smallest != null){
			sum += smallest.nKeys;
			smallest = smallest.right;
		}
		
		return sum;
	} // size

	/***************************************************************************
	 * Print the B+Tree using a pre-order traveral and indenting each level.
	 * 
	 * @param n
	 *            the current node to print
	 * @param level
	 *            the current level of the B+Tree
	 *  
	 *   
	 */
	@SuppressWarnings("unchecked")
	private void print(Node n, int level) {
		out.println("BpTree");
		out.println("-------------------------------------------");

		for (int j = 0; j < level; j++)
			out.print("\t");
		out.print("[ . ");
		for (int i = 0; i < n.nKeys; i++)
			out.print(n.key[i] + " . ");
		out.println("]");
		if (!n.isLeaf) {
			for (int i = 0; i <= n.nKeys; i++)
				print((Node) n.ref[i], level + 1);
		} // if

		out.println("-------------------------------------------");
	} // print

	/***************************************************************************
	 * Recursive helper function for finding a key in B+trees.
	 * 
	 * @param key
	 *            the key to find
	 * @param ney
	 *            the current node
	 * 
	 *  
	 */
	@SuppressWarnings("unchecked")
	private V find(K key, Node n) {
		count++;
		boolean found = false;
		for (int i = 0; i < n.nKeys; i++) {
			K k_i = n.key[i];
			
			if (n.isLeaf){
				if (key.compareTo(k_i) <= 0){
					return (key.equals(k_i)) ? (V) n.ref[i] : null;
				}
			}else{
				if (key.compareTo(k_i) < 0){
					return find(key, (Node) n.ref[i]);
				}else{
					return find(key, (Node) n.ref[i + 1]);
				}
			}
		} // for
		return (n.isLeaf) ? null : find(key, (Node) n.ref[n.nKeys]);
	} // find

	/***************************************************************************
	 * Recursive helper function for inserting a key in B+trees.
	 * 
	 * @param key
	 *            the key to insert
	 * @param ref
	 *            the value/node to insert
	 * @param n
	 *            the current node
	 * @param p
	 *            the parent node
	 *
	 *  
	 */
	private void insert(K key, V ref, Node n, Node p) {
		
		if (n.isLeaf){

			boolean inserted = false;
			if (n.nKeys < ORDER - 1){
				for (int i = 0; i < n.nKeys; i++){
					if (key.compareTo(n.key[i]) < 0){
						wedge(key, ref, n, i);
						inserted = true;
						break;
					}else if (key.compareTo(n.key[i]) == 0 ){
						out.println("BpTree:insert: attempt to insert duplicate key = " + key);
					}
				}
				if (!inserted){
					wedge(key, ref, n, n.nKeys);
				}
			}else {
				Node newNode = split(key, ref, n);
				K tempKey;
				Node rightRef;
				Node leftRef;
				
				Node temp = n.right;
				n.right = newNode;
				newNode.left = n;
				newNode.right = temp;
				
				tempKey = newNode.key[0];
				leftRef = n;
				rightRef = newNode;

				if (p == null){
					Node parent = new Node(false);
					p = parent;
					root = parent;
					n.parent = parent;
					newNode.parent = parent;
					
					parent.ref[0] = n;
					insertIntoParent(tempKey, leftRef, rightRef, parent);
				}else{
					newNode.parent = p;
					insertIntoParent(tempKey, leftRef, rightRef, p);
				}
				
				for (int i = 0; i < p.nKeys; i++){
					if (p.ref[i] == n){
						if (i - 1 >= 0){
							p.key[i - 1] = n.key[0];
						}
						break;
					}
				}
				
			}
			
		}else{
			boolean inserted = false;
			for (int i = 0; i < n.nKeys; i++){
				if (key.compareTo(n.key[i]) < 0){
					insert(key, ref, (Node) n.ref[i], n);
					inserted = true;
					break;
				}
			}
			if (!inserted){
				insert(key, ref, (Node) n.ref[n.nKeys], n);
			}
		}
		
	} // insert

	/*****************************************************************************
	 * This method has a recursive manner from the bottom of the B+ Tree upto a node which does not need spliting.
	 * This is used when a leaf is splited and we need to insert a new value into its parent.
	 * 
	 * @param key The new value to be inserted into the parent
	 * @param leftRef Left reference for that new key
	 * @param rightRef right reference for that new key
	 * @param parent the node which we want to add the new key
	 * 
	 *  
	 */
	private void insertIntoParent(K key, Node leftRef, Node rightRef, Node parent){
		if (parent.nKeys < ORDER - 1){
			if (parent.nKeys == 0){
				parent.ref[0] = leftRef;
			}
			
			boolean inserted = false;
			for (int i = 0; i < parent.nKeys; i++){
				if (key.compareTo(parent.key[i]) < 0){
					wedge(key, (V) rightRef, parent, i);
					inserted = true;
					break;
				}else if (key.compareTo(parent.key[i]) == 0 ){
					out.println("BpTree:insert: attempt to insert duplicate key = " + key);
				}
			}
			if (!inserted){
				wedge(key, (V) rightRef, parent, parent.nKeys);
			}
		}else{
			Node newNode = split(key, (V) rightRef, parent);
			K tempKey;
			Node tempRightRef;
			Node tempLeftRef;
			
			tempKey = newNode.key[0];
			for (int i = 0; i < newNode.nKeys - 1; i++){
				newNode.key[i] = newNode.key[i + 1];
			}
			for (int i = 0; i < newNode.nKeys; i++){
				newNode.ref[i] = newNode.ref[i + 1];
			}
			newNode.nKeys--;

			tempRightRef = newNode;
			tempLeftRef = parent;

			if (parent.parent == null){
				parent.parent = new Node(false);
				root = parent.parent;
			}
			
			insertIntoParent(tempKey, tempLeftRef, tempRightRef, parent.parent);
		}
		
	}
	
	/***************************************************************************
	 * Wedge the key-ref pair into node n.
	 * 
	 * @param key
	 *            the key to insert
	 * @param ref
	 *            the value/node to insert
	 * @param n
	 *            the current node
	 * @param i
	 *            the insertion position within node n
	 *
	 *  
	 */
	private void wedge(K key, V ref, Node n, int i) {
		if (n.isLeaf){
			for (int j = n.nKeys; j > i; j--) {
				n.key[j] = n.key[j - 1];
				n.ref[j] = n.ref[j - 1];
			} // for
			n.key[i] = key;
			n.ref[i] = ref;
			n.nKeys++;
		}else{
			for (int j = n.nKeys; j > i; j--) {
				n.key[j] = n.key[j - 1];
				n.ref[j + 1] = n.ref[j];
			} // for
			n.key[i] = key;
			n.ref[i + 1] = ref;
			n.nKeys++;
		}
	} // wedge
	
	/***************************************************************************
	 * Split node n and return the newly created node.
	 * 
	 * @param key
	 *            the key to insert
	 * @param ref
	 *            the value/node to insert
	 * @param n
	 *            the current node
	 *
	 *  
	 */
	private Node split(K key, V ref, Node n) {
		List<Pair> pairs = new ArrayList<>();
		Object firstRef = n.ref[0];
		
		if (!n.isLeaf){
			for (int i = 0; i < n.nKeys; i++){
				pairs.add(new Pair(n.key[i], n.ref[i + 1]));
			}
		}else{
			for (int i = 0; i < n.nKeys; i++){
				pairs.add(new Pair(n.key[i], n.ref[i]));
			}
		}
		pairs.add(new Pair(key, ref));
		
		Comparator<Pair> comparator = new Comparator<BpTreeMap<K,V>.Pair>() {
			public int compare(Pair o1, Pair o2) {
				return o1.key.compareTo(o2.key);
			}
		};
		Collections.sort(pairs, comparator);

		Node newNode = new Node(n.isLeaf);
		if (!n.isLeaf){
			n.ref[0] = firstRef;
			n.nKeys = 0;
			for (int i = 0; i < ORDER / 2; i++){
				n.key[i] = pairs.get(i).key;
				n.ref[i + 1] = pairs.get(i).ref;
				n.nKeys++;
			}
			
			for (int i = ORDER / 2; i < pairs.size(); i++){
				newNode.key[i - ORDER / 2] = pairs.get(i).key;
				newNode.ref[i - ORDER / 2 + 1] = pairs.get(i).ref;
				newNode.nKeys++;
			}
		}else{
			n.nKeys = 0;
			for (int i = 0; i < ORDER / 2; i++){
				n.key[i] = pairs.get(i).key;
				n.ref[i] = pairs.get(i).ref;
				n.nKeys++;
			}
			
			for (int i = ORDER / 2; i < pairs.size(); i++){
				newNode.key[i - ORDER / 2] = pairs.get(i).key;
				newNode.ref[i - ORDER / 2] = pairs.get(i).ref;
				newNode.nKeys++;
			}
		}
		
		return newNode;
	} // split

	/***************************************************************************
	 * This inner class is used to hold all pairs of Keys and their References for the sorting purpose. 
	 *  
	 */
	private class Pair {
		public K key;
		public Object ref;
		
		public Pair(K key, Object ref){
			this.key = key;
			this.ref = ref;
		}
	}		
	
	/***************************************************************************
	 * The main method used for testing.
	 * 
	 * @param the
	 *            command-line arguments (args [0] gives number of keys to
	 *            insert)
	 *            
	 *  
	 */
	public static void main(String[] args) {
		BpTreeMap<Integer, Integer> bpt = new BpTreeMap<>(Integer.class, Integer.class);
		
		/*int totKeys = 10;
		if (args.length == 1)
			totKeys = Integer.valueOf(args[0]);
		for (int i = 1; i < totKeys; i += 2){
			bpt.put(i, i * i);
		}
			
		bpt.print(bpt.root, 0);
		for (int i = 0; i < totKeys; i++) {
			if (i == 5){
				int  a = 1;
			}
			
			out.println("key = " + i + " value = " + bpt.get(i));
		} // for
*/		
		bpt.put(new Integer(50), new Integer(5000));
		bpt.put(new Integer(10), new Integer(1000));
		bpt.put(new Integer(40), new Integer(4000));
		bpt.put(new Integer(20), new Integer(2000));
		bpt.put(new Integer(60), new Integer(6000));
		bpt.put(new Integer(70), new Integer(7000));
		bpt.put(new Integer(80), new Integer(8000));
		bpt.put(new Integer(90), new Integer(9000));
		bpt.put(new Integer(100), new Integer(10000));
		bpt.put(new Integer(110), new Integer(11000));
		bpt.put(new Integer(120), new Integer(12000));
		bpt.put(new Integer(130), new Integer(13000));
		bpt.put(new Integer(140), new Integer(14000));
		bpt.put(new Integer(150), new Integer(15000));
		
		Integer test;
		test = bpt.get(120);
		out.println("test is "+ test);
		out.println();
		
		
		bpt.print(bpt.root, 0);
		
		out.println("Entry Set:");
		for (Entry e : bpt.entrySet()){
			System.out.println(e.getKey());
		}

		out.println("\n-------------------------------------------\n");
		
		System.out.println("First Key: " + bpt.firstKey());
		System.out.println("Last Key: " + bpt.lastKey());

		out.println("\n-------------------------------------------\n");
		
		out.println("Head map for 56");
		SortedMap resultHead = bpt.headMap(new Integer(56));
		for (Object e : resultHead.entrySet()){
			System.out.println(e);
		}
		
		out.println("\n-------------------------------------------\n");
		
		out.println("Tail map for 94");
		SortedMap resultTail = bpt.tailMap(new Integer(94));
		for (Object e : resultTail.entrySet()){
			System.out.println(e);
		}
		
		out.println("\n-------------------------------------------\n");
		
		out.println("Sub map for 49 to 119");
		SortedMap resultSub = bpt.subMap(new Integer(49), new Integer(119));
		for (Object e : resultSub.entrySet()){
			System.out.println(e);
		}
		
		out.println("\n-------------------------------------------\n");
		out.println("Size: " + bpt.size());
	} // main

} // BpTree class
