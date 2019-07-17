public class Fibonacci {
	public static long fib(int n) {
		if (n == 0) return 0;
		if (n == 1) return 1;

		return fib(n-1) + fib(n-2);
	}

	public static void main(String[] args) {
		long q = 0;
		for (int i = 0; i < 10; i++) {
			q = fib(42);
		}
		System.out.println(q);
	}
}
