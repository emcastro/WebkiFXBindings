package emcastro;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSFunction3<A, B, C, R> {

    R call(A a, B b, C c);

}
