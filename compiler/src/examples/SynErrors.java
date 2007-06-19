package examples;

import compiler;  //1. (warning) Missing Semicolon will be inserted. 

/* Errors:	* on any missing Identifiers
 *          * Illegal tokens like: like Identifier starting with a digit,...
 * 			
 * 
 * Warning: * on any missing ;  ) { } ] and on almost all "(" (exept Object declaration when it has to distinguish between "[" and "(" )
 */

public class SynErrors {	 // (warning) when "class" and "{" is missing
							 

	// (warning) when "static" "(" ")" "{" is missing,
	// (warning) when return type is missing ! (TODO object upper case ?)!
	// (warning) at missing "," in ParameterList
	public static void quickSort(int[] a, int unten, int oben) {  	
		int tmp;													
		int i = unten;
		int j = oben; 
		int tmpSum = unten+oben; 
		int x = a[ tmpSum / 2 ]; // Pivotelement, willkuerlich  // (warning) on missing "]" and missing ";" both are working!
 
		do {;
			while (a[i] < x)
				i++; // x fungiert als Bremse
			while (a[j] > x)
				j--; // x fungiert als Bremse
			if (i <= j) {
				tmp = a[i]; // Hilfsspeicher
				a[i] = a[j]; // a[i] und 
				a[j] = tmp; // a[j] werden getauscht
				i++;
				j--;
			}
		} while (i <= j);
		// alle Elemente der linken Haelfte sind kleiner
		// als alle Elemente der rechten Haelfte 

		if (unten < j)
			quickSort(a, unten, j); // sortiere linke Haelfte
		if (i < oben)
			quickSort(a, i, oben); // sortiere rechte Haelfte
	}

}
