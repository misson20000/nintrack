package net.xenotoad.nintrack;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ListExpression;
import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by misson20000 on 2/25/17.
 */
public class Util {
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static <A, B> ObjectExpression<B> map(ObjectExpression<A> src, Function<A, B> mapper) {
        SimpleObjectProperty<B> prop = new SimpleObjectProperty<>();
        src.addListener((value, oldValue, newValue) -> prop.setValue(mapper.apply(newValue)));
        prop.setValue(mapper.apply(src.getValue()));
        return prop;
    }

    public static <A, B> ObjectExpression<B> mapProperty(ObjectExpression<A> src, Function<A, ObservableValue<B>> mapper) {
        SimpleObjectProperty<B> prop = new SimpleObjectProperty<>();
        src.addListener((value, oldValue, newValue) -> prop.bind(mapper.apply(newValue)));
        prop.bind(mapper.apply(src.getValue()));
        return prop;
    }

    public static <A> BooleanExpression mapBoolean(ObjectExpression<A> src, Function<A, Boolean> mapper) {
        SimpleBooleanProperty prop = new SimpleBooleanProperty();
        src.addListener((value, oldValue, newValue) -> prop.setValue(mapper.apply(newValue)));
        prop.setValue(mapper.apply(src.getValue()));
        return prop;
    }

    public static <A> StringExpression mapStringExpression(ObjectExpression<A> src, Function<A, StringExpression> mapper) {
        SimpleStringProperty prop = new SimpleStringProperty();
        src.addListener((value, oldValue, newValue) -> prop.bind(mapper.apply(newValue)));
        prop.bind(mapper.apply(src.getValue()));
        return prop;
    }

    public static <A, B> ListExpression<B> mapList(ObservableList<A> src, Function<A, B> mapper) {
        SimpleListProperty<B> prop = new SimpleListProperty<>();
        InvalidationListener listener = (o) -> prop.set(FXCollections.observableList(src.stream().map(mapper).collect(Collectors.toList())));
        src.addListener(listener);
        listener.invalidated(src);
        return prop;
    }

    public static <T, A, R> ObjectExpression<R> collectList(ObservableList<T> src, Collector<T, A, R> collector) {
        SimpleObjectProperty<R> prop = new SimpleObjectProperty<R>();
        InvalidationListener listener = (o) -> prop.setValue(src.stream().collect(collector));
        src.addListener(listener);
        listener.invalidated(src);
        return prop;
    }

    public static <A, B> ListExpression<B> flatMapList(ObservableList<A> src, Function<A, Stream<B>> mapper) {
        SimpleListProperty<B> prop = new SimpleListProperty<>();
        InvalidationListener listener = (o) -> prop.set(FXCollections.observableList(src.stream().flatMap(mapper).collect(Collectors.toList())));
        src.addListener(listener);
        listener.invalidated(src);
        return prop;
    }

    public static <T> BooleanExpression anyMatch(ObservableList<T> src, Function<T, BooleanExpression> predicate) {
        SimpleBooleanProperty prop = new SimpleBooleanProperty();
        ListChangeListener<T> listener = (change) -> prop.bind(null);
        src.addListener(listener);
        listener.onChanged(null);
        return prop;
    }

    public static NumberExpression unboxNumber(ObjectExpression<NumberExpression> exp) {
        SimpleDoubleProperty prop = new SimpleDoubleProperty();
        InvalidationListener listener = (o) -> prop.bind(exp.getValue());
        exp.addListener(listener);
        listener.invalidated(exp);
        return prop;
    }

    public static ObservableValue<Double> asDouble(NumberExpression exp) {
        SimpleObjectProperty<Double> prop = new SimpleObjectProperty<>();
        InvalidationListener listener = (o) -> prop.setValue(exp.doubleValue());
        exp.addListener(listener);
        listener.invalidated(exp);
        return prop;
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static Collector<NumberExpression, ObjectProperty<NumberExpression>, NumberExpression> numberSumCollector =
            new Collector<NumberExpression, ObjectProperty<NumberExpression>, NumberExpression>() {
                @Override
                public Supplier<ObjectProperty<NumberExpression>> supplier() {
                    return () -> new SimpleObjectProperty<>(DoubleExpression.doubleExpression(new SimpleDoubleProperty(0)));
                }

                @Override
                public BiConsumer<ObjectProperty<NumberExpression>, NumberExpression> accumulator() {
                    return (accumulator, value) -> accumulator.setValue(accumulator.getValue().add(value));
                }

                @Override
                public BinaryOperator<ObjectProperty<NumberExpression>> combiner() {
                    return (a, b) -> new SimpleObjectProperty<>(Bindings.add(a.getValue(), b.getValue()));
                }

                @Override
                public Function<ObjectProperty<NumberExpression>, NumberExpression> finisher() {
                    return ObjectExpression::getValue;
                }

                @Override
                public Set<Collector.Characteristics> characteristics() {
                    return Collections.emptySet();
                }
            };

    public static Collector<NumberExpression, ObjectProperty<NumberExpression>, NumberExpression> numberSumCollector() {
        return numberSumCollector;
    }
}
