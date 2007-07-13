package examples;

import Util;

public class MainModule {

	public static int calcFibonacci(int a, int b) {
		
		a = a + b;
		return a;
	}
	
	
	public static void main() {
	
		int a = 0;
		int b = 1;
		int tmp;
		
		while (b < 10) {
			
			Util.print(b);
			
			a = calcFibonacci(a, b);
			tmp = b;
			b = a; 
			a = tmp;
			
			
		}
		
	}
	
}
