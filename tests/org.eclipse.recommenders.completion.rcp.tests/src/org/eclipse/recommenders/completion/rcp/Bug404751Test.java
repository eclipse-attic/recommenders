package org.eclipse.recommenders.completion.rcp;

import static org.eclipse.recommenders.tests.CodeBuilder.classDeclaration;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.recommenders.internal.rcp.CachingAstProvider;
import org.eclipse.recommenders.tests.jdt.JavaProjectFixture;
import org.eclipse.recommenders.utils.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

/**
 * Test that generic types and their (first) bound are taken into account when completion is triggered on a generic
 * return value.
 * 
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=404751">Bug 404751</a>
 */
@RunWith(Parameterized.class)
public class Bug404751Test {

    private final String typeParameters;
    private final String typeArguments;
    private final String expectedType;

    public Bug404751Test(String typeParameters, String typeArguments, String expectedType) {
        this.typeParameters = typeParameters;
        this.typeArguments = typeArguments;
        this.expectedType = expectedType;
    }

    @Parameters
    public static Collection<Object[]> scenarios() {
        LinkedList<Object[]> scenarios = Lists.newLinkedList();

        scenarios.add(scenario(null, null, "Object"));
        scenarios.add(scenario(null, "Number", "Number"));

        scenarios.add(scenario("Number", null, "Number"));
        scenarios.add(scenario("Number", "Integer", "Integer"));

        scenarios.add(scenario("List", null, "List"));
        scenarios.add(scenario("List<?>", null, "List"));
        scenarios.add(scenario("List<?>", "List", "List"));
        scenarios.add(scenario("List<?>", "ArrayList", "ArrayList"));
        scenarios.add(scenario("List<?>", "List<?>", "List"));
        scenarios.add(scenario("List<?>", "ArrayList<?>", "ArrayList"));
        scenarios.add(scenario("List<T>", null, "List"));

        scenarios.add(scenario("List & Closeable", null, "List"));
        scenarios.add(scenario("Closeable & List", null, "Closeable"));

        scenarios.add(scenario("Number & List", null, "Number"));
        scenarios.add(scenario("Number & List & Closeable", null, "Number"));

        return scenarios;
    }

    private static Object[] scenario(String tExtends, String t, String expectedType) {
        String typeParameters = tExtends == null ? "<T>" : "<T extends " + tExtends + ">";
        String typeArguments = t == null ? "" : "<" + t + ">";
        return new Object[] { typeParameters, typeArguments, expectedType };
    }

    @Test
    public void testReceiverTypeOfInstanceMethod() throws Exception {
        String producerMethod = "T produce() { return null; }";
        String consumerMethod = "static void consume() { new TestClass" + typeArguments + "().produce().$; }";
        CharSequence code = classDeclaration("class TestClass" + typeParameters, producerMethod + consumerMethod);

        IRecommendersCompletionContext sut = exercise(code);
        IType receiverType = sut.getReceiverType().get();

        assertThat(receiverType.getElementName(), is(equalTo(expectedType)));
    }

    @Test
    public void testReceiverTypeOfStaticMethod() throws Exception {
        String producerMethod = "static " + typeParameters + " T produce() { return null; }";
        String consumerMethod = "static void consume() { TestClass." + typeArguments + "produce().$; }";
        CharSequence code = classDeclaration("class TestClass", producerMethod + consumerMethod);

        IRecommendersCompletionContext sut = exercise(code);
        IType receiverType = sut.getReceiverType().get();

        assertThat(receiverType.getElementName(), is(equalTo(expectedType)));
    }

    private IRecommendersCompletionContext exercise(CharSequence code) throws CoreException {
        JavaProjectFixture fixture = new JavaProjectFixture(ResourcesPlugin.getWorkspace(), "test");
        Pair<ICompilationUnit, Set<Integer>> struct = fixture.createFileAndParseWithMarkers(code.toString());
        ICompilationUnit cu = struct.getFirst();
        int completionIndex = struct.getSecond().iterator().next();
        JavaContentAssistInvocationContext ctx = new JavaContentAssistContextMock(cu, completionIndex);

        return new RecommendersCompletionContext(ctx, new CachingAstProvider());
    }
}
