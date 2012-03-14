package org.eclipse.recommenders.tests.completion.rcp.subwords;

import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import junit.framework.Assert;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.Document;
import org.eclipse.recommenders.internal.completion.rcp.subwords.SubwordsCompletionProposalComputer;
import org.eclipse.recommenders.tests.CodeBuilder;
import org.eclipse.recommenders.tests.SmokeTestScenarios;
import org.eclipse.recommenders.tests.completion.rcp.JavaContentAssistContextMock;
import org.eclipse.recommenders.tests.jdt.JavaProjectFixture;
import org.eclipse.recommenders.utils.Tuple;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.ComparableExtensions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IntegerExtensions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("all")
public class SubwordsCompletionProposalComputerIntegrationTest {
  private static JavaProjectFixture fixture = new Function0<JavaProjectFixture>() {
    public JavaProjectFixture apply() {
      IWorkspace _workspace = ResourcesPlugin.getWorkspace();
      JavaProjectFixture _javaProjectFixture = new JavaProjectFixture(_workspace, "test");
      return _javaProjectFixture;
    }
  }.apply();
  
  private Stopwatch stopwatch = new Function0<Stopwatch>() {
    public Stopwatch apply() {
      Stopwatch _stopwatch = new Stopwatch();
      return _stopwatch;
    }
  }.apply();
  
  private long MAX_COMPUTATION_LIMIT_MILLIS = 2000;
  
  @Test
  public void test000_smoke() {
    List<CharSequence> _scenarios = SmokeTestScenarios.scenarios();
    for (final CharSequence scenario : _scenarios) {
      this.smokeTest(scenario);
    }
  }
  
  @Test
  public void test001() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Object o=\"\"; o.hc$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("hashCode");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test002() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Object o=\"\"; o.c$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("clone", "hashCode", "getClass");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test003() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Object o=\"\"; o.C$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("hashCode", "getClass", "clone");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test004() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Object o=\"\"; o.cod$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("hashCode");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test005() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Object o=\"\"; o.coe$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("hashCode", "clone");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test006() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Object o=\"\"; o.Ce$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("hashCode", "clone");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test007_subtype() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("String s=\"\"; s.lone$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("clone");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test008_overloaded() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Object o=\"\"; o.w$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("wait", "wait", "wait");
      this.exerciseAndVerify(code, _asList);
  }
  
  @Test
  public void test009_ProposedGettersAndSetters() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("String id;$");
      CharSequence _classbody = CodeBuilder.classbody(_builder);
      final CharSequence code = _classbody;
      List<String> _asList = Arrays.<String>asList("getId", "setId");
      this.exerciseAndVerifyLenient(code, _asList);
  }
  
  @Test
  public void test010_ConstuctorCalls() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ConcurrentHashMap b = new ConcurrentHashMap$");
      CharSequence _classbody = CodeBuilder.classbody(_builder);
      final CharSequence code = _classbody;
      List<String> _asList = Arrays.<String>asList("ConcurrentHashMap(int");
      this.exerciseAndVerifyLenient(code, _asList);
  }
  
  @Test
  public void test011_NewCompletionOnCompleteStatement() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("new LinkedLis$t(){};");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<String> _asList = Arrays.<String>asList("LinkedList(");
      this.exerciseAndVerifyLenient(code, _asList);
  }
  
  @Test
  public void test008_ranking() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("String s=\"\"; s.has$");
      CharSequence _method = CodeBuilder.method(_builder);
      final CharSequence code = _method;
      List<IJavaCompletionProposal> _exercise = this.exercise(code);
      final List<IJavaCompletionProposal> actual = _exercise;
      final Function1<IJavaCompletionProposal,Boolean> _function = new Function1<IJavaCompletionProposal,Boolean>() {
          public Boolean apply(final IJavaCompletionProposal p) {
            String _string = p.toString();
            boolean _startsWith = _string.startsWith("hashCode");
            return Boolean.valueOf(_startsWith);
          }
        };
      IJavaCompletionProposal _findFirst = IterableExtensions.<IJavaCompletionProposal>findFirst(actual, _function);
      final IJavaCompletionProposal pHashCode = ((IJavaCompletionProposal) _findFirst);
      final Function1<IJavaCompletionProposal,Boolean> _function_1 = new Function1<IJavaCompletionProposal,Boolean>() {
          public Boolean apply(final IJavaCompletionProposal p) {
            String _string = p.toString();
            boolean _startsWith = _string.startsWith("getChars");
            return Boolean.valueOf(_startsWith);
          }
        };
      IJavaCompletionProposal _findFirst_1 = IterableExtensions.<IJavaCompletionProposal>findFirst(actual, _function_1);
      final IJavaCompletionProposal pGetChars = ((IJavaCompletionProposal) _findFirst_1);
      int _relevance = pGetChars.getRelevance();
      int _relevance_1 = pHashCode.getRelevance();
      boolean _operator_lessThan = IntegerExtensions.operator_lessThan(_relevance, _relevance_1);
      Assert.assertTrue(_operator_lessThan);
  }
  
  /**
   * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=370572
   */
  @Test
  public void testBug370572_1() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("public class SubwordsBug {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("File aFile = null;");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.append("public void m1() {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("afl$");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.append("public void m(File f) {}}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final CharSequence code = _builder;
      ArrayList<String> _newArrayList = CollectionLiterals.<String>newArrayList("aFile");
      ArrayList<String> expected = _newArrayList;
      this.exerciseAndVerify(code, expected);
  }
  
  @Test
  @Ignore("this fails because JDT does not propose anything at m($afl) (note, *we* trigger code completion before the first token))")
  public void testBug370572_2() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("public class SubwordsBug {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("File aFile = null;");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.append("public void m1() {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("m(afl$);");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.newLine();
      _builder.append("\t");
      _builder.append("public void m(File f) {}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final CharSequence code = _builder;
      ArrayList<String> _newArrayList = CollectionLiterals.<String>newArrayList("aFile");
      ArrayList<String> expected = _newArrayList;
      this.exerciseAndVerify(code, expected);
  }
  
  public void smokeTest(final CharSequence code) {
    try {
      {
        SubwordsCompletionProposalComputerIntegrationTest.fixture.clear();
        String _string = code.toString();
        Tuple<ICompilationUnit,Set<Integer>> _createFileAndParseWithMarkers = SubwordsCompletionProposalComputerIntegrationTest.fixture.createFileAndParseWithMarkers(_string);
        final Tuple<ICompilationUnit,Set<Integer>> struct = _createFileAndParseWithMarkers;
        ICompilationUnit _first = struct.getFirst();
        final ICompilationUnit cu = _first;
        Set<Integer> _second = struct.getSecond();
        for (final Integer completionIndex : _second) {
          {
            JavaContentAssistContextMock _javaContentAssistContextMock = new JavaContentAssistContextMock(cu, (completionIndex).intValue());
            final JavaContentAssistContextMock ctx = _javaContentAssistContextMock;
            SubwordsCompletionProposalComputer _subwordsCompletionProposalComputer = new SubwordsCompletionProposalComputer();
            final SubwordsCompletionProposalComputer sut = _subwordsCompletionProposalComputer;
            sut.sessionStarted();
            Stopwatch _stopwatch = new Stopwatch();
            this.stopwatch = _stopwatch;
            this.stopwatch.start();
            CompletionProposalCollector _completionProposalCollector = new CompletionProposalCollector(cu, false);
            cu.codeComplete((completionIndex).intValue(), _completionProposalCollector);
            NullProgressMonitor _nullProgressMonitor = new NullProgressMonitor();
            sut.computeCompletionProposals(ctx, _nullProgressMonitor);
            this.stopwatch.stop();
          }
        }
      }
    } catch (Exception _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void exerciseAndVerifyLenient(final CharSequence code, final List<String> expected) {
      List<IJavaCompletionProposal> _exercise = this.exercise(code);
      final List<IJavaCompletionProposal> actual = _exercise;
      for (final String e : expected) {
        {
          final Function1<IJavaCompletionProposal,Boolean> _function = new Function1<IJavaCompletionProposal,Boolean>() {
              public Boolean apply(final IJavaCompletionProposal p) {
                String _string = p.toString();
                boolean _startsWith = _string.startsWith(e);
                return Boolean.valueOf(_startsWith);
              }
            };
          IJavaCompletionProposal _findFirst = IterableExtensions.<IJavaCompletionProposal>findFirst(actual, _function);
          final IJavaCompletionProposal match = _findFirst;
          Assert.assertNotNull(match);
          this.applyProposal(match, code);
          actual.remove(match);
          boolean _isEmpty = actual.isEmpty();
          if (_isEmpty) {
            return;
          }
        }
      }
  }
  
  public void applyProposal(final IJavaCompletionProposal proposal, final CharSequence code) {
      String _string = code.toString();
      Document _document = new Document(_string);
      final Document doc = _document;
      proposal.apply(doc);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("applying template ");
      _builder.append(proposal, "");
      _builder.append(" on code ");
      _builder.append(code, "");
      _builder.append(" failed.");
      String _string_1 = _builder.toString();
      String _get = doc.get();
      int _length = _get.length();
      int _length_1 = code.length();
      boolean _operator_greaterThan = IntegerExtensions.operator_greaterThan(_length, _length_1);
      Assert.assertTrue(_string_1, _operator_greaterThan);
  }
  
  public void exerciseAndVerify(final CharSequence code, final List<String> expected) {
      List<IJavaCompletionProposal> _exercise = this.exercise(code);
      final List<IJavaCompletionProposal> actual = _exercise;
      StringConcatenation _builder = new StringConcatenation();
      _builder.append(" ");
      _builder.append("some expected values were not found.\\nExpected: ");
      _builder.append(expected, " ");
      _builder.append(",\\nFound: ");
      _builder.append(actual, " ");
      _builder.append(" ");
      String _string = _builder.toString();
      int _size = expected.size();
      int _size_1 = actual.size();
      Assert.assertEquals(_string, _size, _size_1);
      for (final String e : expected) {
        {
          final Function1<IJavaCompletionProposal,Boolean> _function = new Function1<IJavaCompletionProposal,Boolean>() {
              public Boolean apply(final IJavaCompletionProposal p) {
                String _string = p.toString();
                boolean _startsWith = _string.startsWith(e);
                return Boolean.valueOf(_startsWith);
              }
            };
          IJavaCompletionProposal _findFirst = IterableExtensions.<IJavaCompletionProposal>findFirst(actual, _function);
          final IJavaCompletionProposal match = _findFirst;
          Assert.assertNotNull(match);
          this.applyProposal(match, code);
          actual.remove(match);
        }
      }
  }
  
  public List<IJavaCompletionProposal> exercise(final CharSequence code) {
    try {
      {
        SubwordsCompletionProposalComputerIntegrationTest.fixture.clear();
        String _string = code.toString();
        Tuple<ICompilationUnit,Set<Integer>> _createFileAndParseWithMarkers = SubwordsCompletionProposalComputerIntegrationTest.fixture.createFileAndParseWithMarkers(_string);
        final Tuple<ICompilationUnit,Set<Integer>> struct = _createFileAndParseWithMarkers;
        ICompilationUnit _first = struct.getFirst();
        final ICompilationUnit cu = _first;
        Set<Integer> _second = struct.getSecond();
        Integer _head = IterableExtensions.<Integer>head(_second);
        final Integer completionIndex = _head;
        JavaContentAssistContextMock _javaContentAssistContextMock = new JavaContentAssistContextMock(cu, (completionIndex).intValue());
        final JavaContentAssistContextMock ctx = _javaContentAssistContextMock;
        SubwordsCompletionProposalComputer _subwordsCompletionProposalComputer = new SubwordsCompletionProposalComputer();
        final SubwordsCompletionProposalComputer sut = _subwordsCompletionProposalComputer;
        sut.sessionStarted();
        this.stopwatch.start();
        NullProgressMonitor _nullProgressMonitor = new NullProgressMonitor();
        List _computeCompletionProposals = sut.computeCompletionProposals(ctx, _nullProgressMonitor);
        final List actual = _computeCompletionProposals;
        this.stopwatch.stop();
        return actual;
      }
    } catch (Exception _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public Object failIfComputerTookTooLong(final CharSequence code) {
    Object _xifexpression = null;
    long _elapsedMillis = this.stopwatch.elapsedMillis();
    boolean _operator_greaterThan = ComparableExtensions.<Long>operator_greaterThan(Long.valueOf(_elapsedMillis), Long.valueOf(this.MAX_COMPUTATION_LIMIT_MILLIS));
    if (_operator_greaterThan) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("completion took FAR too long: ");
      long _elapsedMillis_1 = this.stopwatch.elapsedMillis();
      _builder.append(_elapsedMillis_1, "");
      _builder.append("\\n in:\\n");
      _builder.append(code, "");
      String _string = _builder.toString();
      Assert.fail(_string);
    }
    return _xifexpression;
  }
}
