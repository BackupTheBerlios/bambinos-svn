package examples;

import test1.test2;

public class TestStuff {

	int a = 2;
	int[] b = int[9];
	
	public static int output(int value){

		//print(value);
		
	}
	
	
	public static void calc(int a, int b, int c) {
		
		int result = 2 + (3 + 4) * (a + 3 * ((3*b) * (4+c)));
		
		output(result);
	}
	

	public static void compare(int a, int b, char c, char d) {
	
		int x = 0;
		int y = 1;
	
		if (((((a*20) > (b+b+1)) && (c == d)) && (c == 'a')) || (c == 'a')) {
			print(x);
		} else {
			print(y);
		}
	
		if (c == d) {
			print(x);
		} else {
			print(y);
		}
	
	
	}
	
	public static void main(String[] args) {
		
		int a=5;
		int b=6;
		int c=7;
		
		char x = 'a';
		char y = 'b';
		
		if (a == 5) {
			calc(a, b, c);
		} else {
			print(a);
		}
		
		compare(a,a,x,x);
		
	}

}
