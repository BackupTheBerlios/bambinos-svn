package examples;

public class SynErrors {

	public static void quickSort(int[] a, int unten, int oben) {
		int tmp;
		int i = unten;
		int j = oben;
		int tmp_sum = unten + oben ; 
		int x = a[ tmp_sum / 2]; // Pivotelement, willkuerlich

		do {
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
