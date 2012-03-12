package org.eclipse.recommenders.tests.completion.rcp.chain

import java.util.List
import org.apache.commons.lang3.StringUtils
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.recommenders.internal.completion.rcp.chain.ChainCompletionProposal
import org.eclipse.recommenders.internal.completion.rcp.chain.ChainCompletionProposalComputer
import org.eclipse.recommenders.tests.CodeBuilder
import org.eclipse.recommenders.tests.completion.rcp.JavaContentAssistContextMock
import org.eclipse.recommenders.tests.completion.rcp.RecommendersCompletionContextFactoryMock
import org.eclipse.recommenders.tests.jdt.JavaProjectFixture
import org.junit.Ignore
import org.junit.Test

import static junit.framework.Assert.*
import static org.eclipse.recommenders.tests.SmokeTestScenarios.*
import static org.eclipse.recommenders.tests.completion.rcp.chain.ChainCompletionScenariosTest.*
import org.junit.Before
 
class ChainCompletionScenariosTest { 
  
	static JavaProjectFixture fixture = new JavaProjectFixture(ResourcesPlugin::getWorkspace(),"test")
	
	@Before
	def void before(){
		fixture.clear
	}
	
 	@Test
	def void smokeTestScenarios(){
		for(scenario : scenarios){
			val struct = fixture.createFileAndParseWithMarkers(scenario)
			val cu = struct.first;
	
			for(completionIndex : struct.second){
				val ctx = new org.eclipse.recommenders.tests.completion.rcp.JavaContentAssistContextMock(cu, completionIndex)
				val sut = new org.eclipse.recommenders.internal.completion.rcp.chain.ChainCompletionProposalComputer(new RecommendersCompletionContextFactoryMock())
				sut.sessionStarted
				sut.computeCompletionProposals(ctx, null)
			}
		}
	}
 
 
	@Test
	def void testFindLocalAnchor(){
		val code = '''
		import java.util.concurrent.*;
		public class MyClass {
			void test() {
				ExecutorService pool = Executors.newCachedThreadPool();
				Future future = $
			}
		}
		'''
		var expected = w(newArrayList(
			"pool submit", // different args
			"pool submit", // different args
			"pool submit"  // different args
			))
			
		exercise(code, expected); 
	}
	 
	@Test
	def void testFindLocalAnchorWithIsExactMatch() {
		// well, not really an exact match...!
		val code = '''
		import java.util.*;
		class MyClass {
			void m(){
				List<Object> findMe;
				List<String> l = $
			}
		}''' 
		
		// need to def expectations
		var expected = w(newArrayList(
			"findMe",
			"findMe subList"
			))
		exercise(code, expected);
	}
	
	
	@Test
	def void testFindFieldAnchor(){
		val code = '''
		import java.util.concurrent.*;
		public class MyClass {
			ExecutorService pool;
			void test() {
				Future future = $
			}
		}
		'''
		var expected = w(newArrayList(
			"pool submit", // different args
			"pool submit", // different args
			"pool submit"  // different args
			))
			
		exercise(code, expected);
	}

	@Test
	def void testFindArrayFieldAnchor(){
		val code = '''
		import java.util.concurrent.*;
		public class MyClass {
			ExecutorService pool[];
			void test() {
				Future future = $
			}
		}
		'''
		var expected = w(newArrayList(
			"pool submit", // different args
			"pool submit", // different args
			"pool submit"  // different args
			))
			
		exercise(code, expected);
	}
	@Test
	def void testFindMultiDimArrayField(){
		val code = '''
		import java.util.concurrent.*;
		public class MyClass {
			ExecutorService pool[][][];
			void test() {
				Future future = $
			} 
		}
		'''
		var expected = w(newArrayList(
			"pool submit", // different args
			"pool submit", // different args
			"pool submit"  // different args
			))
		exercise(code, expected);
	}

	@Test
	def void testFindFieldInSuperType() {
		val code = '''
		import java.util.*;
		import java.awt.*;
		class MyClass extends Event{
			void m(){
				Event e = $
			}
		}''' 
		
		// need to def expectations
		var expected = w(newArrayList(
			"evt"
			))
		exercise(code, expected);
	}
	
	@Test
	@Ignore ("too many solutions - more than 200!")
	def void testCompletionOnRuntime() {
		val code = '''
		import java.io.*;
		class MyClass {
			void m(){
				InputStream in = Runtime.$
			}
		}''' 
		
		// need to def expectations
		var expected = w(newArrayList(
			"getRuntime getLocalizedInputStream",
			"findMe subList"
			))
		exercise(code, expected);
	}
	
	@Test
	def void testCompletionOnLocaVariable() {
		val code = '''
		import java.util.*;
		class MyClass {
			void m(){
				List<Object> findMe;
				List<String> l = findMe.$
			}
		}'''
		
		// need to def expectations
		var expected = w(newArrayList(
			"subList"
			))
		exercise(code, expected);
	}
	@Test
	@Ignore("fails on build server")
	def void testCompletionOnStaticType() {
		val code = '''
		import java.util.*;
		class MyClass {
			void m(){
				List<String> l = Collections.$
			}
		}''' 
		
		// need to def expectations
		var expected = w(newArrayList(
			"list",
			"list subList",
			"unmodifiableList" , 
			"unmodifiableList subList",
			"synchronizedList", 
			"synchronizedList subList",
			"checkedList",
			"checkedList subList",
			"emptyList",
			"emptyList subList",
			"singletonList", 
			"singletonList subList",
			"nCopies",
			"nCopies subList",
			"EMPTY_LIST",
			"EMPTY_LIST subList"
			))
		exercise(code, expected);
	}
	
	@Test
	def void testCompletionOnReturnStatement() {
		val code = '''
		import java.util.*;
		class MyClass {
			List<String> m(){
				List<Object> l;
				return $
			}
		}''' 
		
		// need to def expectations
		var expected = w(newArrayList(
			"l",
			"l subList",
			"m",
			"m subList"
			))
		exercise(code, expected);
	}
	
	@Test
	def void testCompletionOnEnumDoesNotThrowNPE() {
		val code = '''
		import java.lang.annotation.*;
		import java.util.*;
		class MyClass {
			void m(){
				String s = Annotation.$
			}
		}''' 
		
		// don't expect anything.
		var expected = w(newArrayList())
		exercise(code, expected);
	}
	
	
	/**
	 * we had some trouble with supertype hierarchy. This test that we do not generate 
	 * any chains that return a supertype of the requested type (ExecutorService in this case)
	 */
	@Test
	def void testFindSelfAssignment(){
		val code = '''
		import java.util.concurrent.*;
		public class MyClass {
			ThreadPoolExecutor pool;
			void test() {
				pool = $
			}
		}
		'''
		var expected = w(newArrayList(
			"pool"
			))
			
		exercise(code, expected);
	}
	
	// TODO we should qualify the proposed field with "this." 
	@Test
	def void testFindMatchingSubtypeForAssignment(){
		val code = '''
		import java.util.concurrent.*;
		public class MyClass {
			ThreadPoolExecutor pool;
			void test() {
				ExecutorService pool = $
			}
		}
		'''
		var expected = w(newArrayList(
			"pool"
			))
			
		exercise(code, expected);
	}

	
	@Test
	def void testCompletionOnFieldField(){
		val code = '''
		import java.awt.*;
		public class MyClass {
			Event e;
			void test() {
				Event evt = e.evt.$
			}
		}
		'''
		var expected = w(newArrayList(
			"evt"
			))
			
		exercise(code, expected);
	}
	
	@Test
	def void testPrefixFilter(){
		val code = '''
		import java.awt.*;
		public class MyClass {
			Event evt;
			Event aevt;
			void test() {
				Event evt = a$
			}
		}
		'''
		var expected = w(newArrayList(
			"aevt","aevt evt"
			))
			
		exercise(code, expected);
	}
	
	
	@Test
	def void test012(){
		compile(FIELDS)
		val code = CodeBuilder::classbody(
			'''
				public static Fields f = new Fields();
				public static void test_protected() {
				final Boolean c = $
			''')
		var expected = w(newArrayList(
			"f _public"
			))
			
		exercise(code, expected);
	}
	
	def exercise(CharSequence code, List<? extends List<String>> expected){
		val struct = fixture.createFileAndParseWithMarkers(code.toString)
		val cu = struct.first;
		val completionIndex = struct.second.head
		val ctx = new org.eclipse.recommenders.tests.completion.rcp.JavaContentAssistContextMock(cu, completionIndex)
		
		val sut = new org.eclipse.recommenders.internal.completion.rcp.chain.ChainCompletionProposalComputer(new RecommendersCompletionContextFactoryMock())
		sut.sessionStarted
		val proposals = sut.computeCompletionProposals(ctx, null) 
		 for(proposal : proposals){
			val names = (proposal as org.eclipse.recommenders.internal.completion.rcp.chain.ChainCompletionProposal).getChainElementNames
			assertTrue('''couldn't find «names» in expected.'''.toString, expected.remove(names))
		} 
		assertTrue(''' some expected values were not found «expected» '''.toString, expected.empty)
	}
	
	
	def compile(CharSequence code){
		fixture.createFileAndParseWithMarkers(code)
	}


	def l(String spaceSeparatedElementNames){ 
		val elementNames = StringUtils::split(spaceSeparatedElementNames);
		return newArrayList(elementNames) as List<String>
	}
	def  w (String [] chains){
		val List<List<String>> res = newArrayList();
		for(chain :chains){
			res.add(l(chain))
		}
		return res as List<List<String>>
	}
	
	CharSequence FIELDS = 
		'''
		public class Fields {
			public static 			Boolean _spublic;
			protected static 		Boolean _sprotected;
			/* default */ static 	Boolean _sdefault;
			private static 			Boolean _sprivate;

			public 			Boolean _public;
			protected 		Boolean _protected;
			/* default */	Boolean _default;
			private 		Boolean _private;
		}
		'''
}