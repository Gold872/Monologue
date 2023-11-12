package monologue;

import monologue.evaluation.FieldEval;
import monologue.evaluation.MethodEval;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Monologue {

  public static final NTLogger ntLogger = new NTLogger();
  public static final DataLogger dataLogger = new DataLogger();
  public static final Map<Logged, String> loggedRegistry = new HashMap<Logged, String>();

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  public @interface LogFile {
    public boolean once() default false;

    public LogLevel level() default LogLevel.RELEASE;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  public @interface LogNT {
    public boolean once() default false;

    public LogLevel level() default LogLevel.RELEASE;
  }

  /**
   * Is just an alias for {@link LogNT} now until removed
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  // @Deprecated(since = "1.1.0", forRemoval = true)
  public @interface LogBoth {
    /** Does nothing */
    // @Deprecated(since = "1.1.0", forRemoval = true)
    public String path() default "";

    public boolean once() default false;

    public LogLevel level() default LogLevel.RELEASE;
  }

  /**
   * Annotate a field or method in a {@link Logged} subclass and ({@link MonoShuffleboardTab} or {@link MonoShuffleboardLayout})
   * with this to log it to shuffleboard.
   * 
   * <p>
   * Supported Types(primitive or not): Double, Boolean, String, Integer,
   * Double[], Boolean[], String[], Integer[], Sendable, Pose2d, Pose3d,
   * Rotation2d,
   * Rotation3d, Translation2d, Translation3d
   * 
   * @param pos       [optional] the position of the widget on the shuffleboard
   * @param size      [optional] the size of the widget on the shuffleboard
   * @param widget    [optional] the widget type to use
   * @param level     [optional] the log level to use
   * 
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD })
  public @interface MonoShuffleboard {
    /** {Column, Row} | */
    public int[] pos() default {};

    /** {Width, Height} | */
    public int[] size() default {};

    public String widget() default "";

    public LogLevel level() default LogLevel.RELEASE;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  public @interface MonoShuffleboardTab {}

  /**
   * @param pos       [optional] the position of the widget on the shuffleboard
   * @param size      [optional] the size of the widget on the shuffleboard
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  public @interface MonoShuffleboardLayout {
    /** {Column, Row} | */
    public int[] pos() default {};

    /** {Width, Height} | */
    public int[] size() default {};
  }

  /**
   * Is the main entry point for the monologue library.
   * It will interate over every member of the provided Logged object and
   * evaluated if it should be logged to the network tables or to a file.
   * 
   * Will also recursively check field values for classes that implement Logged
   * and log those as well.
   * 
   * @param loggable the root Logged object to log
   * @param rootPath the nt/datalog path to log to
   */
  public static void setupMonologue(Logged loggable, String rootPath) {
    logObj(loggable, rootPath);
  }

  /**
   * @param loggable the obj to scrape
   * @param path the path to log to
   */
  public static void logObj(Logged loggable, String path) {
    var lpath = LogPath.from(path);
    if (!lpath.isValid()) {
      throw new IllegalArgumentException("Invalid path: " + path);
    } else if (lpath.isRoot()) {
      throw new IllegalArgumentException("Root path of / is not allowed");
    }
    loggedRegistry.put(loggable, path);
    for (Field field : getAllFields(loggable.getClass())) {
      FieldEval.evalField(field, loggable, path);
    }

    for (Method method : getAllMethods(loggable.getClass())) {
      MethodEval.evalMethod(method, loggable, path);
    }
  }

  public static void updateAll() {
    ntLogger.update();
    dataLogger.update();
    ShuffleboardApi.run();
  }

  private static List<Field> getAllFields(Class<?> type) {
    Set<Field> result = new HashSet<Field>();

    result.addAll(Arrays.asList(type.getDeclaredFields()));
    result.addAll(Arrays.asList(type.getFields()));

    return result.stream().toList();
  }

  private static List<Method> getAllMethods(Class<?> type) {
    Set<Method> result = new HashSet<Method>();

    result.addAll(Arrays.asList(type.getDeclaredMethods()));
    result.addAll(Arrays.asList(type.getMethods()));

    return result.stream().toList();
  }
}
