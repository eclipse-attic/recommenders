package org.eclipse.recommenders.completion.rcp.utils;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.eclipse.recommenders.completion.rcp.it.TestUtils.createRecommendersCompletionContext;
import static org.eclipse.recommenders.testing.CodeBuilder.*;
import static org.eclipse.recommenders.utils.names.VmMethodName.get;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.recommenders.completion.rcp.CompletionContextKey;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.VmMethodName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@SuppressWarnings("restriction")
@RunWith(Parameterized.class)
public class ProposalUtilsTest {

    private static final IMethodName METHOD_VOID = VmMethodName.get("LExample.method()V");
    private static final IMethodName METHOD_OBJECT = VmMethodName.get("LExample.method(Ljava/lang/Object;)V");
    private static final IMethodName METHOD_NUMBER = VmMethodName.get("LExample.method(Ljava/lang/Number;)V");
    private static final IMethodName METHOD_COLLECTION = VmMethodName.get("LExample.method(Ljava/util/Collection;)V");
    private static final IMethodName SET_INT_STRING = VmMethodName
            .get("Ljava/util/List.set(ILjava/lang/Object;)Ljava/lang/Object;");

    private static final IMethodName NESTED_METHOD_VOID = VmMethodName.get("LExample$Nested.method()V");

    private static final IMethodName METHOD_INTS = VmMethodName.get("LExample.method([I)V");
    private static final IMethodName METHOD_OBJECTS = VmMethodName.get("LExample.method([Ljava/lang/Object;)V");

    private static final IMethodName INIT = VmMethodName.get("LExample.<init>()V");
    private static final IMethodName INIT_OBJECT = VmMethodName.get("LExample.<init>(Ljava/lang/Object;)V");
    private static final IMethodName INIT_NUMBER = VmMethodName.get("LExample.<init>(Ljava/lang/Number;)V");
    private static final IMethodName INIT_COLLECTION = VmMethodName.get("LExample.<init>(Ljava/util/Collection;)V");

    private static final IMethodName NESTED_INIT = VmMethodName.get("LExample$Nested.<init>()V");
    private static final IMethodName NESTED_INIT_OBJECT = VmMethodName
            .get("LExample$Nested.<init>(Ljava/lang/Object;)V");
    private static final IMethodName NESTED_INIT_NUMBER = VmMethodName
            .get("LExample$Nested.<init>(Ljava/lang/Number;)V");
    private static final IMethodName NESTED_INIT_COLLECTION = VmMethodName
            .get("LExample$Nested.<init>(Ljava/util/Collection;)V");
    private static final IMethodName INNER_INIT_EXAMPLE = VmMethodName.get("LExample$Inner.<init>(LExample;)V");
    private static final IMethodName INNER_INIT_EXAMPLE_OBJECT = VmMethodName
            .get("LExample$Inner.<init>(LExample;Ljava/lang/Object;)V");
    private static final IMethodName INNER_INIT_EXAMPLE_NUMBER = VmMethodName
            .get("LExample$Inner.<init>(LExample;Ljava/lang/Number;)V");
    private static final IMethodName INNER_INIT_EXAMPLE_COLLECTION = VmMethodName
            .get("LExample$Inner.<init>(LExample;Ljava/util/Collection;)V");

    private static final IMethodName COMPARE_TO_BOOLEAN = VmMethodName
            .get("Ljava/lang/Boolean.compareTo(Ljava/lang/Boolean;)I");
    private static final IMethodName COMPARE_TO_OBJECT = VmMethodName
            .get("Ljava/lang/Comparable.compareTo(Ljava/lang/Object;)I");

    private static final IMethodName OBJECT_HASH_CODE = VmMethodName.get("Ljava/lang/Object.hashCode()I");
    private static final IMethodName EXAMPLE_HASH_CODE = VmMethodName.get("LExample.hashCode()I");

    private static final IMethodName OBJECT_CLONE = VmMethodName.get("Ljava/lang/Object.clone()Ljava/lang/Object;");

    private final CharSequence code;
    private final IMethodName expectedMethod;

    public ProposalUtilsTest(CharSequence code, IMethodName expectedMethod) {
        this.code = code;
        this.expectedMethod = expectedMethod;
    }

    @Parameters
    public static Collection<Object[]> scenarios() {
        LinkedList<Object[]> scenarios = Lists.newLinkedList();

        scenarios.add(scenario(classbody("Example", "void method() { this.method$ }"), METHOD_VOID));
        scenarios.add(scenario(classbody("Example", "void method(Object o) { this.method$ }"), METHOD_OBJECT));
        scenarios.add(scenario(classbody("Example", "void method(Collection c) { this.method$ }"), METHOD_COLLECTION));

        scenarios.add(scenario(classbody("Example", "void method(int[] is) { this.method$ }"), METHOD_INTS));
        scenarios.add(scenario(classbody("Example", "void method(Object[] os) { this.method$ }"), METHOD_OBJECTS));

        scenarios.add(scenario(classbody("Example", "static class Nested { void method() { this.method$ } }"),
                NESTED_METHOD_VOID));
        scenarios.add(scenario(classbody("Example<T>", "static class Nested { void method() { this.method$ } }"),
                NESTED_METHOD_VOID));
        scenarios.add(scenario(classbody("Example", "static class Nested<T> { void method() { this.method$ } }"),
                NESTED_METHOD_VOID));

        scenarios.add(scenario(classbody("Example", "void method(Collection<Number> c) { this.method$ }"),
                METHOD_COLLECTION));
        scenarios
                .add(scenario(classbody("Example", "void method(Collection<?> c) { this.method$ }"), METHOD_COLLECTION));
        scenarios.add(scenario(classbody("Example", "void method(Collection<? extends Number> c) { this.method$ }"),
                METHOD_COLLECTION));
        scenarios.add(scenario(classbody("Example", "void method(Collection<? super Number> c) { this.method$ }"),
                METHOD_COLLECTION));

        scenarios.add(scenario(classbody("Example<T>", "void method(T t) { this.method$ }"), METHOD_OBJECT));
        scenarios.add(scenario(classbody("Example<O extends Object>", "void method(O o) { this.method$ }"),
                METHOD_OBJECT));
        scenarios.add(scenario(classbody("Example<N extends Number>", "void method(N n) { this.method$ }"),
                METHOD_NUMBER));
        scenarios
                .add(scenario(classbody("Example<N extends Number & Comparable>", "void method(N n) { this.method$ }"),
                        METHOD_NUMBER));

        scenarios.add(scenario(classbody("Example<L extends List<String>>", "void method(L l) { l.set$ }"),
                SET_INT_STRING));

        String auxiliaryDefinition = "class Auxiliary<L extends List<String>> { <N extends L> void method(N n) { } }";
        scenarios.add(scenario(classbody("Example", "void method(Auxiliary a) { a.method$ }") + auxiliaryDefinition,
                get("LAuxiliary.method(Ljava/util/List;)V")));

        scenarios.add(scenario(classbody("Example<T>", "void method(T[] t) { this.method$ }"), METHOD_OBJECTS));
        scenarios.add(scenario(classbody("Example<O extends Object>", "void method(O[] o) { this.method$ }"),
                METHOD_OBJECTS));

        scenarios.add(scenario(classbody("Example<N extends Number>", "void method(Collection<N> c) { this.method$ }"),
                METHOD_COLLECTION));

        scenarios.add(scenario(classbody("Example", "<T> void method(T t) { this.method$ }"), METHOD_OBJECT));
        scenarios.add(scenario(classbody("Example", "<O extends Object> void method(O o) { this.method$ }"),
                METHOD_OBJECT));
        scenarios.add(scenario(classbody("Example", "<N extends Number> void method(N n) { this.method$ }"),
                METHOD_NUMBER));
        scenarios.add(scenario(
                classbody("Example", "<N extends Number & Comparable> void method(N n) { this.method$ }"),
                METHOD_NUMBER));

        scenarios.add(scenario(classbody("Example", "static <T> void method(T t) { Example.<Integer>method$ }"),
                METHOD_OBJECT));
        scenarios.add(scenario(
                classbody("Example", "static <O extends Object> void method(O o) { Example.<Integer>method$ }"),
                METHOD_OBJECT));
        scenarios.add(scenario(
                classbody("Example", "static <N extends Number> void method(N n) { Example.<Integer>method$ }"),
                METHOD_NUMBER));
        scenarios.add(scenario(
                classbody("Example",
                        "static <N extends Number & Comparable> void method(N n) { Example.<Integer>method$ }"),
                METHOD_NUMBER));

        scenarios.add(scenario(classbody("Example", "<T> void method(T[] t) { this.method$ }"), METHOD_OBJECTS));
        scenarios.add(scenario(classbody("Example", "<O extends Object> void method(O[] o) { this.method$ }"),
                METHOD_OBJECTS));

        scenarios.add(scenario(classbody("Example", "void method(Boolean b) { b.compareTo$ }"), COMPARE_TO_BOOLEAN));
        scenarios.add(scenario(classbody("Example", "void method(Delayed d) { d.compareTo$ }"), COMPARE_TO_OBJECT));

        scenarios.add(scenario(classbody("Example", "Example() { this($) }"), INIT));
        scenarios.add(scenario(classbody("Example<T>", "Example(T t) { this($) }"), INIT_OBJECT));
        scenarios.add(scenario(classbody("Example<T extends Object>", "Example(T t) { this($) }"), INIT_OBJECT));
        scenarios.add(scenario(classbody("Example<N extends Number>", "Example(N n) { this($) }"), INIT_NUMBER));
        scenarios.add(scenario(classbody("Example<N>", "Example(Collection<? extends N> c) { this($) }"),
                INIT_COLLECTION));

        // Using nested classes to speed up JDT's constructor completion; this avoids timeouts.
        scenarios.add(scenario(classbody("Example", "static class Nested { Nested() { new Example.Nested$ } }"),
                NESTED_INIT));
        scenarios.add(scenario(classbody("Example", "static class Nested<T> { Nested(T t) { new Example.Nested$ } }"),
                NESTED_INIT_OBJECT));
        scenarios.add(scenario(
                classbody("Example", "static class Nested<T extends Object> { Nested(T t) { new Example.Nested$ } }"),
                NESTED_INIT_OBJECT));
        scenarios.add(scenario(
                classbody("Example", "static class Nested<N extends Number> { Nested(N n) { new Example.Nested$ } }"),
                NESTED_INIT_NUMBER));
        scenarios.add(scenario(
                classbody("Example",
                        "static class Nested<N> { Nested(Collection<? extends N> c) { new Example.Nested$ } }"),
                NESTED_INIT_COLLECTION));

        scenarios.add(scenario(classbody("Example", "class Inner { Inner() { new Example.Inner$ } }"),
                INNER_INIT_EXAMPLE));
        scenarios.add(scenario(classbody("Example", "class Inner<T> { Inner(T t) { new Example.Inner$ } }"),
                INNER_INIT_EXAMPLE_OBJECT));
        scenarios.add(scenario(
                classbody("Example", "class Inner<T extends Object> { Inner(T t) { new Example.Inner$ } }"),
                INNER_INIT_EXAMPLE_OBJECT));
        scenarios.add(scenario(
                classbody("Example", "class Inner<N extends Number> { Inner(N n) { new Example.Inner$ } }"),
                INNER_INIT_EXAMPLE_NUMBER));
        scenarios.add(scenario(
                classbody("Example", "class Inner<N> { Inner(Collection<? extends N> c) { new Example.Inner$ } }"),
                INNER_INIT_EXAMPLE_COLLECTION));

        scenarios.add(scenario(classbody("Example implements Comparable", "compareTo$"), COMPARE_TO_OBJECT));
        scenarios.add(scenario(classbody("Example implements Comparable<Example>", "compareTo$"), COMPARE_TO_OBJECT));
        scenarios.add(scenario(classbody("Example<T> implements Comparable<T>", "compareTo$"), COMPARE_TO_OBJECT));
        scenarios.add(scenario(classbody("Example<N extends Number> implements Comparable<N>", "compareTo$"),
                COMPARE_TO_OBJECT));

        scenarios.add(scenario(classbody("Example<T extends Throwable>", "void method() throws T { this.method$ }"),
                METHOD_VOID));
        scenarios.add(scenario(classbody("Example", "<T extends Throwable> void method() throws T { this.method$ }"),
                METHOD_VOID));

        scenarios.add(scenario(classbody("Example", "int hashCode() { hashCode$ }"), EXAMPLE_HASH_CODE));
        scenarios.add(scenario(classbody("Example", "int hashCode() { this.hashCode$ }"), EXAMPLE_HASH_CODE));
        scenarios.add(scenario(classbody("Example", "int hashCode() { super.hashCode$ }"), OBJECT_HASH_CODE));

        scenarios
                .add(scenario(method("new Object() { int hashCode() { return super.hashCode$ } };"), OBJECT_HASH_CODE));

        scenarios.add(scenario(method("new Object[0].hashCode$"), OBJECT_HASH_CODE));

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=442723
        scenarios.add(scenario(method("this.clone$"), OBJECT_CLONE));
        scenarios.add(scenario(method("new Object[0].clone$"), OBJECT_CLONE));
        scenarios.add(scenario(method("new Object[0][0].clone$"), OBJECT_CLONE));
        scenarios.add(scenario(method("new String[0].clone$"), OBJECT_CLONE));
        scenarios.add(scenario(classbody("Example<T>", "void method() { new T[0].clone$ }"), OBJECT_CLONE));

        return scenarios;
    }

    private static Object[] scenario(CharSequence compilationUnit, IMethodName expectedMethod) {
        return new Object[] { compilationUnit, expectedMethod };
    }

    @Test
    public void test() throws Exception {
        IRecommendersCompletionContext context = createRecommendersCompletionContext(code);
        Collection<CompletionProposal> proposals = context.getProposals().values();
        Optional<LookupEnvironment> environment = context.get(CompletionContextKey.LOOKUP_ENVIRONMENT);
        IMethodName actualMethod = ProposalUtils.toMethodName(getOnlyElement(proposals), environment.orNull()).get();

        assertThat(actualMethod, is(equalTo(expectedMethod)));
    }
}
