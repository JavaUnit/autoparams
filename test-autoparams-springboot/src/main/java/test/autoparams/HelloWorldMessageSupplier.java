package test.autoparams;

public class HelloWorldMessageSupplier implements MessageSupplier {

    @Override
    public String getMessage() {
        return "hello world";
    }
}
