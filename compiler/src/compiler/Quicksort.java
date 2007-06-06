public 112{

	static int k = 0;

	public static int partition(int[] a, int first, int last){
		int pivot;
		int unknown;
		int separator;
		pivot = a[first];
		
		separator = first;
		
		unknown = separator + 1;

		while (unknown <= last){

			if (a[unknown] < pivot) {
				a[separator] = a[unknown];
				separator = separator + 1;
				a[unknown] = a[separator];
			}

			unknown = unknown + 1;
		}

		a[separator] = pivot;
		return separator;
	}
	

	public static void quicksortInterval(int[] a, int first, int last){
		int splitPoint;
		if (first < last){
			
			splitPoint = partition(a, first, last);
			quicksortInterval(a, first, splitPoint - 1);
			quicksortInterval(a, splitPoint + 1, last);
		}
	}

	
	public static void quicksort(int[] a, int x){
		quicksortInterval(a, 0, x - 1);
	}

}