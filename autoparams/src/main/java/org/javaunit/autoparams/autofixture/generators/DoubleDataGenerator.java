package org.javaunit.autoparams.autofixture.generators;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public class DoubleDataGenerator implements DataGenerator {

    @Override
    public boolean isSupport(Class clazz) {
        return clazz.equals(double.class) || clazz.equals(Double.class);
    }

    @Override
    public Optional<Object> generate(Class clazz, ParameterizedType parameterizedType) {
        return Optional.of(RANDOM.nextDouble());
    }

}
