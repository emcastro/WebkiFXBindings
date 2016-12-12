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

}
