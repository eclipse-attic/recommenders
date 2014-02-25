package org.eclipse.recommenders.models.advisors;

import static org.eclipse.recommenders.models.DependencyType.JAR;
import static org.eclipse.recommenders.models.DependencyType.PROJECT;
import static org.eclipse.recommenders.tests.models.utils.FolderUtils.dir;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.eclipse.recommenders.models.DependencyInfo;
import org.eclipse.recommenders.models.IProjectCoordinateAdvisor;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.tests.models.utils.FolderUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class AndroidDirectoryNameAdvisorTest {

    private final DependencyInfo dependency;

    private final Optional<ProjectCoordinate> expectedCoordinate;

    public AndroidDirectoryNameAdvisorTest(String description, DependencyInfo dependency,
            Optional<ProjectCoordinate> projectCoordinate) {
        this.dependency = dependency;
        this.expectedCoordinate = projectCoordinate;
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> scenarios() {
        LinkedList<Object[]> scenarios = Lists.newLinkedList();

        scenarios.add(scenario("Standard path, earliest SDK: Android 1.0",
                jar(dir("home", "example", "android-sdks", "platforms", "android-1"), "android.jar"),
                ProjectCoordinate.valueOf("com.google.android:android:1.0.0")));

        scenarios.add(scenario("Standard path, patch-level SDK: Android 2.3.3 Gingerbread",
                jar(dir("home", "example", "android-sdks", "platforms", "android-10"), "android.jar"),
                ProjectCoordinate.valueOf("com.google.android:android:2.3.3")));

        scenarios.add(scenario("Standard path, latest SDK: Android 4.4 Kit Kat",
                jar(dir("home", "example", "android-sdks", "platforms", "android-19"), "android.jar"),
                ProjectCoordinate.valueOf("com.google.android:android:4.4.0")));

        scenarios.add(scenario("Standard path, future SDK",
                jar(dir("home", "example", "android-sdks", "platforms", "android-20"), "android.jar"), null));

        scenarios.add(scenario("Non-standard path, latest SDK: Android 4.4 Kit Kat",
                jar(dir("home", "example", "android-sdks", "platforms", "android-20"), "android-20.jar"), null));

        return scenarios;
    }

    private static DependencyInfo jar(File dir, String name) {
        File jar = new File(dir, name);
        return new DependencyInfo(jar, JAR);
    }

    @Test
    public void testScenario() {
        IProjectCoordinateAdvisor sut = new AndroidDirectoryNameAdvisor();

        Optional<ProjectCoordinate> result = sut.suggest(dependency);

        assertThat(result, is(expectedCoordinate));
    }

    private static Object[] scenario(String description, DependencyInfo dependency,
            ProjectCoordinate expectedProjectCoordinate) {
        return new Object[] { description, dependency, Optional.fromNullable(expectedProjectCoordinate) };
    }
}
