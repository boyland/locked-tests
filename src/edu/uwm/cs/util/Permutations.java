package edu.uwm.cs.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author boyland
 * 
 * As by Robert Sedgewick in Permutation Generation Methods (via Bernardo Sulzbach)
 * @param <T>
 */
public class Permutations<T> implements Iterator<T[]> {

	private final T[] array;
	private int remaining;
	private int[] c;
	private int i = -1;
	
	
	public Permutations(T[] a) {
		array = a;
		remaining = a.length;
		for (int i=1; i < a.length; ++i) {
			remaining *= i;
		}
		c = new int[a.length];
	}
	
	private void swap(int i, int j) {
		T tmp = array[i];
		array[i] = array[j];
		array[j] = tmp;
	}
	
	@Override
	public boolean hasNext() {
		return remaining > 0;
	}

	@Override
	public T[] next() {
		if (!hasNext()) throw new NoSuchElementException("no more");
		if (i == -1) {
			i = 0;
			--remaining;
			return array;
		}
		for (;;) {
			if (c[i] < i) {
				if (i % 2 == 0) {
					swap(0,i);
				} else {					
					swap(c[i],i);
				}
				c[i] += 1;
				i = 0;
				--remaining;
				return array;
			} else {
				c[i] = 0;
				i += 1;
			}
		}
	}
	
	public static void main(String[] args) {
		Iterator<String[]> it = new Permutations<>(new String[]{"a","b","c","d"});
		while (it.hasNext()) {
			System.out.println(Arrays.toString(it.next()));
		}
	}
}
