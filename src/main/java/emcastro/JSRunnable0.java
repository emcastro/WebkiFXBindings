package emcastro;

/**
 * Created by ecastro on 05/12/16.
 */
@JSInterface
public interface JSRunnable0 extends JSFunction {

    void call();

    @Override
    default Object invoke(Object[] arguments) {
        checkArity(arguments, 0);
        call();
        return null;
    }
}
