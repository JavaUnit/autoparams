package autoparams;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import autoparams.AnnotationScanner.Edge;
import autoparams.customization.ArgumentProcessing;
import autoparams.customization.ArgumentProcessor;
import autoparams.customization.Customizer;
import autoparams.generator.ParameterQuery;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.commons.util.AnnotationUtils;

import static autoparams.AnnotationConsumption.consumeAnnotationIfMatch;
import static autoparams.AnnotationScanner.scanAnnotations;
import static autoparams.Instantiator.instantiate;

final class TestParameterContext implements ParameterContext {

    private final ResolutionContext resolutionContext;
    private final Parameter parameter;
    private final int index;

    public TestParameterContext(
        ResolutionContext resolutionContext,
        Parameter parameter,
        int index
    ) {
        this.resolutionContext = resolutionContext;
        this.parameter = parameter;
        this.index = index;
    }

    @Override
    public Parameter getParameter() {
        return parameter;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Optional<Object> getTarget() {
        return Optional.empty();
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annotationType) {
        return AnnotationUtils.isAnnotated(
            this.parameter,
            this.index,
            annotationType
        );
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(
        Class<A> annotationType
    ) {
        return AnnotationUtils.findAnnotation(
            this.parameter,
            this.index,
            annotationType
        );
    }

    @Override
    public <A extends Annotation> List<A> findRepeatableAnnotations(
        Class<A> annotationType
    ) {
        return AnnotationUtils.findRepeatableAnnotations(
            this.parameter,
            this.index,
            annotationType
        );
    }

    public Object resolveArgument(Arguments asset) {
        Object supplied = supplyArgument(asset);
        Object argument = convertArgument(supplied);
        consumeArgument(argument);
        return argument;
    }

    private Object supplyArgument(Arguments asset) {
        return index < asset.get().length
            ? asset.get()[index]
            : resolutionContext.resolve(getQuery());
    }

    private ParameterQuery getQuery() {
        Type type = parameter.getParameterizedType();
        return new ParameterQuery(parameter, index, type);
    }

    private Object convertArgument(Object source) {
        if (source instanceof Named<?>) {
            return convertArgument((Named<?>) source);
        } else {
            return getConverter().convert(source, this);
        }
    }

    private Object convertArgument(Named<?> source) {
        Object payload = convertArgument(source.getPayload());
        return Named.of(source.getName(), payload);
    }

    private ArgumentConverter getConverter() {
        return resolutionContext.resolve(ArgumentConverter.class);
    }

    private void consumeArgument(Object argument) {
        if (argument instanceof Named<?>) {
            consumeArgument((Named<?>) argument);
            return;
        }

        for (Edge<ArgumentProcessing> edge :
            scanAnnotations(parameter, ArgumentProcessing.class)
        ) {
            consumeArgument(argument, edge);
        }
    }

    private void consumeArgument(Named<?> argument) {
        consumeArgument(argument.getPayload());
    }

    private void consumeArgument(
        Object argument,
        Edge<ArgumentProcessing> edge
    ) {
        ArgumentProcessor processor = instantiate(edge.getCurrent().value());
        edge.useParent(parent -> consumeAnnotationIfMatch(processor, parent));
        Customizer customizer = processor.process(parameter, argument);
        resolutionContext.applyCustomizer(customizer);
    }
}
