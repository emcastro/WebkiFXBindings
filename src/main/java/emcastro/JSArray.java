package emcastro;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSArray<A> {

    @ArrayGetter
    A get(int index);

    @ArraySetter
    void set(int index, A value);

    @Getter
    int length();

    //TODO implement default stream and iterator


    class Instance<B> implements JSArray<B> {

        public Instance(Object... array) {
            this.array = array;
        }

        private Object[] array;

        @Override
        public B get(int index) {
            return (B) array[index];
        }

        @Override
        public void set(int index, B value) {
            array[index] = value;
        }

        @Override
        public int length() {
            return array.length;
        }
    }

}
