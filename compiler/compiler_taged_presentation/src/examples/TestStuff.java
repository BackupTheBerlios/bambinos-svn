package examples;

public class TestStuff {
	
	
	public static int Fibonacci(int first, int second){

		int z = first+second;
		
		print(z);
		
		Fibonacci(second,z);
		
	}
	
	public static void main(String[] args) {
		
		int r=0;
		int t=1;

		Fibonacci(r, t);
		
	}

}
