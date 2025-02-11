package org.checkerframework.javacutil;

import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

/** This file contains basic utility functions. */
public class SystemUtil {

  /** Do not instantiate. */
  private SystemUtil() {
    throw new Error("Do not instantiate.");
  }

  /** The major version number of the Java runtime (JRE), such as 8, 11, or 17. */
  @SuppressWarnings("deprecation") // remove @SuppressWarnings when getJreVersion() isn't deprecated
  public static final int jreVersion = getJreVersion();

  // Keep in sync with BCELUtil.java (in the bcel-util project).
  /**
   * Returns the major version number from the "java.version" system property, such as 8, 11, or 17.
   *
   * <p>This is different from the version passed to the compiler via {@code --release}; use {@link
   * #getReleaseValue(ProcessingEnvironment)} to get that version.
   *
   * <p>Two possible formats of the "java.version" system property are considered. Up to Java 8,
   * from a version string like `1.8.whatever`, this method extracts 8. Since Java 9, from a version
   * string like `11.0.1`, this method extracts 11.
   *
   * <p>Starting in Java 9, there is the int {@code Runtime.version().feature()}, but that does not
   * exist on JDK 8.
   *
   * @return the major version of the Java runtime
   * @deprecated use field {@link #jreVersion} instead
   */
  @Deprecated // 2022-07-14 not for removal, just to make private (and then it won't be
  // deprecated)
  public static int getJreVersion() {
    String version = System.getProperty("java.version");

    // Up to Java 8, from a version string like "1.8.whatever", extract "8".
    if (version.startsWith("1.")) {
      return Integer.parseInt(version.substring(2, 3));
    }

    // Since Java 9, from a version string like "11.0.1" or "11-ea" or "11u25", extract "11".
    // The format is described at http://openjdk.org/jeps/223 .
    Pattern newVersionPattern = Pattern.compile("^(\\d+).*$");
    Matcher newVersionMatcher = newVersionPattern.matcher(version);
    if (newVersionMatcher.matches()) {
      String v = newVersionMatcher.group(1);
      assert v != null : "@AssumeAssertion(nullness): inspection";
      return Integer.parseInt(v);
    }

    throw new RuntimeException("Could not determine version from property java.version=" + version);
  }

  /**
   * Returns the release value passed to the compiler or null if release was not passed.
   *
   * @param env the ProcessingEnvironment
   * @return the release value or null if none was passed
   */
  public static @Nullable String getReleaseValue(ProcessingEnvironment env) {
    Context ctx = ((JavacProcessingEnvironment) env).getContext();
    Options options = Options.instance(ctx);
    return options.get(Option.RELEASE);
  }

  /**
   * Returns the pathname to the tools.jar file, or null if it does not exist. Returns null on Java
   * 9 and later.
   *
   * @return the pathname to the tools.jar file, or null
   */
  public static @Nullable String getToolsJar() {

    if (jreVersion > 8) {
      return null;
    }

    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      String javaHomeProperty = System.getProperty("java.home");
      if (javaHomeProperty.endsWith(File.separator + "jre")) {
        javaHome = javaHomeProperty.substring(javaHomeProperty.length() - 4);
      } else {
        // Could also determine the location of javac on the path...
        throw new Error("Can't infer Java home; java.home=" + javaHomeProperty);
      }
    }
    File toolsJarFile = new File(new File(javaHome, "lib"), "tools.jar");
    if (!toolsJarFile.exists()) {
      throw new Error(
          String.format(
              "File does not exist: %s ; JAVA_HOME=%s ; java.home=%s",
              toolsJarFile, javaHome, System.getProperty("java.home")));
    }
    return toolsJarFile.toString();
  }
}
