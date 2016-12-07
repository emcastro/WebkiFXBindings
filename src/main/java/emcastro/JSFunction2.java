package emcastro;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction2<A, B, R> {

    R call(A a, B b);

}
