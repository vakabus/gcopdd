import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class MergeSort {
    public static void mergesort(int[] array) {
        if (array.length <= 1) return;

        // split
        int[] firstHalf = Arrays.copyOfRange(array, 0, array.length / 2);
        int[] secondHalf = Arrays.copyOfRange(array, array.length / 2, array.length);

        // recurse
        mergesort(firstHalf);
        mergesort(secondHalf);

        // merge
        int f = 0;
        int s = 0;
        for (int i = 0; i < array.length; i++) {
            if ((f < firstHalf.length && s < secondHalf.length && firstHalf[f] < secondHalf[s]) || s == secondHalf.length) {
                array[i] =  firstHalf[f];
                f++;
            } else {
                array[i] = secondHalf[s];
                s++;
            }
        }
    }

    public static int[] DATA = new Random().ints().limit(1024*128).toArray();

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            var data = Arrays.copyOf(DATA, DATA.length);
            mergesort(data);
        }
    }
}
