package examples;

import test1.test2;

public class TestStuff {

	int a = 2;
	int[] b = int[9];
	
	public static int output(int value){

		println(value);
		
	}
	
	
	public static void calc(int a, int b, int c) {
		
		int result = 2 + (3 + 4) * (a + 3 * ((3*b) * (4+c)));
		
		output(result);
	}
	

	public static void compare(int a, int b, char c, char d) {
	
		int x = 0;
		int y = 1;
		int z = 4;
		/*
		//if (((((a*20) > (b+b+1)) && (c == d)) && (c == 'a') && (true))) {
		if (false || true) {
			output(x);
		} else {
			output(y);
		}
		*/
		while (x <= 10) {
			x = x + 'a';
			output(x);
		}
		output(x);
	
		x = 'd';
		
		/*
		if (c == d) {
			output(x);
		} else {
			output(y);
		}
	*/
	
	}
	
	public static void main() {
		
		int a=5;
		int b=6;
		int c=7;
		
		char x = 'a';
		char y = 'b';
		
		if (a == 5) {
			calc(a, b, c);
		} else {
			output(a);
		}
		
		compare(a,a,x,x);
		
	}

}
