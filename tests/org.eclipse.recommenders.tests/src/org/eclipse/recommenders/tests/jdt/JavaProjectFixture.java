/**
 * Copyright (c) 2010, 2011 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Kevin Munk - Extension for package creation and some helper methods.
 */
package org.eclipse.recommenders.tests.jdt;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertTrue;
import static org.eclipse.recommenders.tests.jdt.AstUtils.MARKER;
import static org.eclipse.recommenders.tests.jdt.AstUtils.MARKER_ESCAPE;
import static org.eclipse.recommenders.utils.Checks.cast;
import static org.eclipse.recommenders.utils.Checks.ensureIsTrue;
import static org.eclipse.recommenders.utils.Throws.throwUnhandledException;
import static org.eclipse.recommenders.utils.Tuple.newTuple;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.recommenders.utils.Tuple;

import com.google.common.collect.Sets;

public class JavaProjectFixture {

    public static String findClassName(final CharSequence source) {
        Pattern p = Pattern.compile(".*?class\\s+" + getJavaIdentifierRegEx() + ".*", Pattern.DOTALL);
        Matcher matcher = p.matcher(source);
        if (!matcher.matches()) {
            p = Pattern.compile(".*interface\\s+" + getJavaIdentifierRegEx() + ".*", Pattern.DOTALL);
            matcher = p.matcher(source);
        }
        assertTrue(matcher.matches());
        final String group = matcher.group(1);
        return group;
    }

    public static List<String> findInnerClassNames(final CharSequence source) {
        String declaringType = findClassName(source);
        List<String> names = newArrayList();

        Pattern p = Pattern.compile("(class|interface)\\s+" + getJavaIdentifierRegEx(), Pattern.DOTALL);
        Matcher matcher = p.matcher(source);
        while (matcher.find()) {
            final String name = matcher.group(2);
            if (!name.equals(declaringType)) {
                names.add(declaringType + "$" + name);
            }
        }
        return names;
    }

    public static List<String> findAnonymousClassNames(final CharSequence source) {
        String declaringType = findClassName(source);
        int num = 1;
        List<String> names = newArrayList();

        // new <name> ( ... ) {
        Pattern p = Pattern
                .compile("new\\s*?" + getJavaIdentifierRegEx() + "\\s*?\\([^)]*?\\)\\s*?\\{", Pattern.DOTALL);
        Matcher matcher = p.matcher(source);
        while (matcher.find()) {
            final String name = matcher.group(1);
            if (!name.equals(declaringType)) {
                names.add(declaringType + "$" + num++);
            }
        }
        return names;
    }

    /**
     * Finds the package name from the package declaration inside the source code.
     *
     * @param source
     *            the source code
     * @return the package name or "" if no package declaration was found
     */
    public static String findPackageName(final CharSequence source) {
        Pattern p = Pattern.compile(".*" // any characters at the beginning
                + "package\\s+" // package declaration
                + "(" // beginning of the package name group
                + getJavaIdentifierRegEx() // the first part of the package
                + "{1}" // must occur one time
                + "([.]{1}" // the following parts of the package must begin with a dot
                + getJavaIdentifierRegEx() // followed by a java identifier
                + ")*" // the (.identifier) group can occur multiple times or not at all
                + ")" // closing of the package name group
                + "[;]+.*", // the following ; and the rest of the source code
                Pattern.DOTALL);
        Matcher matcher = p.matcher(source);
        if (matcher.matches()) {
            final String group = matcher.group(1);
            return group;
        }
        return "";
    }

    /**
     * Returns a regular expression group that can be used to match Java identifier. Java identifier can not start with
     * a digit, but can contain underscore and dollar signs.
     *
     * @return a regular expression group for matching Java identifier
     */
    private static String getJavaIdentifierRegEx() {
        return "([a-zA-Z_$]{1}[a-zA-Z_$0-9]*)";
    }

    private IJavaProject javaProject;
    private ASTParser parser;

    public JavaProjectFixture(final IWorkspace workspace, final String projectName) {
        createJavaProject(workspace, projectName);
        createParser();
    }

    private void createJavaProject(final IWorkspace workspace, final String projectName) {
        final IProject project = workspace.getRoot().getProject(projectName);
        final IWorkspaceRunnable populate = new IWorkspaceRunnable() {

            @Override
            public void run(final IProgressMonitor monitor) throws CoreException {
                createAndOpenProject(project);

                if (!hasJavaNature(project)) {
                    addJavaNature(project);
                    configureProjectClasspath();
                }
            }

            private void createAndOpenProject(final IProject project) throws CoreException {
                if (!project.exists()) {
                    project.create(null);
                }
                project.open(null);
            }

            private boolean hasJavaNature(final IProject project) throws CoreException {
                final IProjectDescription description = project.getDescription();
                final String[] natures = description.getNatureIds();
                return ArrayUtils.contains(natures, JavaCore.NATURE_ID);
            }

            private void configureProjectClasspath() throws JavaModelException {
                final Set<IClasspathEntry> entries = newHashSet();
                final IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
                final IClasspathEntry defaultJREContainerEntry = JavaRuntime.getDefaultJREContainerEntry();
                entries.addAll(asList(rawClasspath));
                entries.add(defaultJREContainerEntry);
                final IClasspathEntry[] entriesArray = entries.toArray(new IClasspathEntry[entries.size()]);
                javaProject.setRawClasspath(entriesArray, null);
            }

            private void addJavaNature(final IProject project) throws CoreException {
                final IProjectDescription description = project.getDescription();
                final String[] natures = description.getNatureIds();
                final String[] newNatures = ArrayUtils.add(natures, JavaCore.NATURE_ID);
                description.setNatureIds(newNatures);
                project.setDescription(description, null);
                javaProject = JavaCore.create(project);

            }
        };
        try {
            workspace.run(populate, null);
        } catch (final Exception e) {
            throwUnhandledException(e);
        }
        javaProject = JavaCore.create(project);
    }

    private void createParser() {
        parser = ASTParser.newParser(AST.JLS3);
        // parser.setEnvironment(...) enables bindings resolving
        parser.setProject(javaProject); // enables bindings and IJavaElement
                                        // resolving
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
    }

    public Tuple<CompilationUnit, Set<Integer>> parseWithMarkers(final String content) {
        final Tuple<String, Set<Integer>> contentMarkersPair = findMarkers(content);
        final String contentWoMarkers = contentMarkersPair.getFirst();
        final Set<Integer> markers = contentMarkersPair.getSecond();
        final CompilationUnit cu = parse(contentWoMarkers);
        return newTuple(cu, markers);
    }

    public Tuple<String, Set<Integer>> findMarkers(final CharSequence content) {
        final Set<Integer> markers = Sets.newTreeSet();
        int pos = 0;
        final StringBuilder sb = new StringBuilder(content);
        while ((pos = sb.indexOf(MARKER, pos)) != -1) {
            sb.deleteCharAt(pos);
            markers.add(pos);
            ensureIsTrue(pos < sb.length());
            pos--;
        }
        return newTuple(sb.toString(), markers);
    }

    public CompilationUnit parse(final String content) {
        parser.setSource(content.toCharArray());
        parser.setUnitName(findClassName(content) + ".java");
        final CompilationUnit cu = cast(parser.createAST(null));
        return cu;
    }

    /**
     * Creates the file with the content in the default package folder. The markers in the content will be removed
     * beforehand. The package specified in the content will not be created. After creation of the file the project will
     * be refreshed and built.
     *
     * @param contentWithMarkers
     *            the code with markers(see {@link AstUtils}.MARKER)
     * @return the Tuple of the ICompilationUnit and the List of marker positions in the code provided
     * @throws CoreException
     */
    public Tuple<ICompilationUnit, Set<Integer>> createFileAndParseWithMarkers(final CharSequence contentWithMarkers)
            throws CoreException {
        final Tuple<String, Set<Integer>> content = findMarkers(contentWithMarkers);

        final ICompilationUnit cu = createFile(content.getFirst(), false);
        refreshAndBuildProject();

        return Tuple.newTuple(cu, content.getSecond());
    }

    /**
     * Creates the package folders and the file with the content inside of this package. The markers in the content will
     * be removed beforehand. After creation of the file the project will be refreshed and built.
     *
     * @param contentWithMarkers
     *            the code with markers(see {@link AstUtils}.MARKER)
     * @return the Tuple of the ICompilationUnit and the List of marker positions in the code provided
     * @throws CoreException
     */
    public Tuple<ICompilationUnit, Set<Integer>> createFileAndPackageAndParseWithMarkers(
            final CharSequence contentWithMarkers) throws CoreException {
        final Tuple<String, Set<Integer>> content = findMarkers(contentWithMarkers);

        createPackage(content.getFirst());
        final ICompilationUnit cu = createFile(content.getFirst(), true);
        refreshAndBuildProject();

        return Tuple.newTuple(cu, content.getSecond());
    }

    /**
     * Refreshes the resources of this project and initiates a full build.
     *
     * @throws CoreException
     */
    public void refreshAndBuildProject() throws CoreException {
        final IProject project = javaProject.getProject();
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    }

    /**
     * Creates the folders that represent the package/s defined in the source string. If the package name was not found,
     * no folders will be created. If some or all of the folders exist, these will not be overwritten. After the
     * creation of the folders, the internal java project will be refreshed.
     *
     * @param content
     *            the content of the file which package declaration will be used to create the package/s.
     * @throws CoreException
     */
    public void createPackage(String content) throws CoreException {
        // get package from the code
        String packageName = findPackageName(content);

        if (!packageName.equalsIgnoreCase("")) {
            final IProject project = javaProject.getProject();

            // append project and package folders
            IPath projectPath = project.getLocation().addTrailingSeparator();

            String relativeFilePath = packageName.replace('.', Path.SEPARATOR);
            relativeFilePath += String.valueOf(Path.SEPARATOR);

            // create package folders
            IPath packagePath = new Path(projectPath.toString() + relativeFilePath);
            File packageDirectory = packagePath.toFile();
            packageDirectory.mkdirs();

            // refresh to prevent that the file creation fails
            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        }
    }

    /**
     * Creates the compilation unit with the class name found in the content. If the content has a package declaration
     * the class will be put inside of this package. For this the package must be exist. The project will not be
     * refreshed and built after creation of this file.<br>
     * <br>
     * To create a file that has markers in it, use the method createFileAndParseWithMarkers() or
     * createFileAndPackageAndParseWithMarkers().
     *
     * @see createPackage(String)
     * @see refreshAndBuildProject()
     * @param content
     *            the content of the compilation unit. Must be java source code with or without package declaration but
     *            with a java class definition
     * @param usePackage
     *            if the package as declared in the content will be used to create the file. Means, if a package
     *            declaration exists in the content this file will be created inside of this package, otherwise the
     *            default package will be used.
     * @return the created compilation compilation unit
     * @throws CoreException
     */
    public ICompilationUnit createFile(final String content, boolean usePackage) throws CoreException {
        final IProject project = javaProject.getProject();

        // get filename
        final String fileName = findClassName(content) + ".java";
        StringBuilder relativeFilePath = new StringBuilder();

        if (usePackage) {
            // get package from the code
            String packageName = findPackageName(content);
            if (!packageName.equalsIgnoreCase("")) {
                relativeFilePath.append(packageName.replace('.', Path.SEPARATOR));
                relativeFilePath.append(String.valueOf(Path.SEPARATOR));
            }
        }

        // add the file name and get the file
        relativeFilePath.append(fileName);
        final IPath path = new Path(relativeFilePath.toString());
        final IFile file = project.getFile(path);

        // delete file
        if (file.exists())
            file.delete(true, new NullProgressMonitor());

        // create file
        final ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes());
        file.create(is, true, new NullProgressMonitor());
        final ICompilationUnit cu = (ICompilationUnit) javaProject.findElement(path);

        return cu;
    }

    /**
     * Goes through the project and deletes all Java and Class files.
     *
     * @throws CoreException
     */
    public void clear() throws CoreException {

        final IProject project = javaProject.getProject();
        project.accept(new IResourceVisitor() {
            @Override
            public boolean visit(final IResource resource) throws CoreException {
                switch (resource.getType()) {
                case IResource.FILE:
                    if (resource.getName().endsWith(".class") || resource.getName().endsWith(".java")) {
                        resource.delete(true, new NullProgressMonitor());
                    }
                }
                return true;
            }
        });
    }

    /**
     * Deletes the project inclusive content from the disk. <b>Warning:</b> This Fixture is no longer usable after doing
     * this.
     *
     * @throws CoreException
     */
    public void deleteProject() throws CoreException {
        javaProject.getProject().delete(true, true, new NullProgressMonitor());
    }

    /**
     * Retrieves the inner java project managed by this fixture.
     *
     * @return the inner java project managed by this fixture
     */
    public IJavaProject getJavaProject() {
        return javaProject;
    }

    /**
     * Removes all markers from the content.
     *
     * @param content
     *            where the markers will be removed
     * @return the content without any markers
     */
    public String removeMarkers(final String content) {
        return content.replaceAll(MARKER_ESCAPE, "");
    }

}
