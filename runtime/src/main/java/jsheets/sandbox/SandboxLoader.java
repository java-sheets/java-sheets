package jsheets.sandbox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;
import jsheets.sandbox.validation.Analysis;
import jsheets.sandbox.validation.Rule;

public final class SandboxLoader implements LoaderDelegate {
  public static SandboxLoader create(Collection<Rule> rules) {
    Objects.requireNonNull(rules, "rules");
    return new SandboxLoader(rules);
  }

  private final SandboxLoader.RemoteClassLoader loader;
  private final Map<String, Class<?>> types = new HashMap<>();
  private final Collection<Rule> rules;

  private SandboxLoader(Collection<Rule> rules) {
    this.loader = new RemoteClassLoader();
    this.rules = rules;
  }

  public void install() {
    Thread.currentThread().setContextClassLoader(loader);
  }

  @Override
  public void load(ExecutionControl.ClassBytecodes[] binaries)
    throws ExecutionControl.ClassInstallException
  {
    loadBinaries(binaries);
    preload(binaries);
  }

  private void loadBinaries(ExecutionControl.ClassBytecodes[] binaries)
    throws ExecutionControl.ClassInstallException
  {
    var analysis = Analysis.create();
    var check = SandboxBytecodeCheck.withRules(rules);
    loadBinariesWithAnalysis(analysis, check, binaries);
    checkAnalysisReport(analysis);
  }

  private void checkAnalysisReport(Analysis analysis) {
    analysis.reportViolations();
  }

  private void loadBinariesWithAnalysis(
    Analysis analysis,
    SandboxBytecodeCheck check,
    ExecutionControl.ClassBytecodes[] binaries
  ) throws ExecutionControl.ClassInstallException{
    try {
      for (var binary : binaries) {
        check.run(analysis, binary.bytecodes());
        loader.declare(binary.name(), binary.bytecodes());
      }
    } catch (Throwable failure) {
      throw new ExecutionControl.ClassInstallException(
        "declare: " + failure.getMessage(),
        new boolean[0]
      );
    }
  }

  private void preload(ExecutionControl.ClassBytecodes[] binaries)
    throws ExecutionControl.ClassInstallException
  {
    boolean[] loaded = new boolean[binaries.length];
    try {
      for ( int index = 0; index < binaries.length; ++index ) {
        var code = binaries[index];
        var type = loader.loadClass(code.name());
        types.put(code.name(), type);
        loaded[index] = true;
        preload(type);
      }
    } catch (Throwable failure) {
      throw new ExecutionControl.ClassInstallException("load: " + failure.getMessage(),
        loaded
      );
    }
  }

  private void preload(Class<?> type) {
    type.getDeclaredMethods();
  }

  @Override
  public void classesRedefined(ExecutionControl.ClassBytecodes[] binaries) {
    for (var binary : binaries) {
      loader.declare(binary.name(), binary.bytecodes());
    }
  }

  @Override
  public void addToClasspath(String classPath) throws ExecutionControl.InternalException {
    try {
      for (var path : classPath.split(File.pathSeparator)) {
        loader.addURL(new File(path).toURI().toURL());
      }
    } catch (Exception failure) {
      throw new ExecutionControl.InternalException(failure.toString());
    }
  }

  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    var type = types.get(name);
    if (type == null) {
      throw new ClassNotFoundException(name + " not found");
    }
    return type;
  }

  private record FileRecord(byte[] content, long timestamp) {}

  private static class RemoteClassLoader extends URLClassLoader {
    private final Map<String, FileRecord> files = new HashMap<>();

    RemoteClassLoader() {
      super(new URL[0]);
    }

    void declare(String name, byte[] bytes) {
      files.put(
        createResourceKeyForClassName(name),
        new FileRecord(bytes, System.currentTimeMillis())
      );
    }

    private String createResourceKeyForClassName(String name) {
      return name.replace('.', '/') + ".class";
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      var file = files.get(createResourceKeyForClassName(name));
      if (file == null) {
        return super.findClass(name);
      }
      return super.defineClass(
        name,
        file.content,
        0,
        file.content.length,
        (CodeSource) null
      );
    }

    @Override
    public URL findResource(String name) {
      var resource = lookupResource(name);
      return resource != null ? resource : super.findResource(name);
    }

    private URL lookupResource(String name) {
      if (!files.containsKey(name)) {
        return null;
      }
      try {
        return new URL(
          /* context */ null,
          new URI("jshell", null, "/" + name, null).toString(),
          new ResourceUrlStreamHandler(name)
        );
      } catch (MalformedURLException | URISyntaxException failure) {
        throw new InternalError(failure);
      }
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
      var resource = lookupResource(name);
      var parentResources = super.findResources(name);
      return resource == null
        ? parentResources
        : plus(parentResources, resource);
    }

    private static <T> Enumeration<T> plus(Enumeration<T> enumeration, T element) {
      var result = new ArrayList<T>();
      while (enumeration.hasMoreElements()) {
        result.add(enumeration.nextElement());
      }
      result.add(element);
      return Collections.enumeration(result);
    }

    @Override
    public void addURL(URL url) {
      super.addURL(url);
    }

    private class ResourceUrlStreamHandler extends URLStreamHandler {
      private final String name;

      ResourceUrlStreamHandler(String name) {
        this.name = name;
      }

      @Override
      protected URLConnection openConnection(URL resource)  {
        return new URLConnection(resource) {
          private InputStream input;
          private Map<String, List<String>> fields;
          private List<String> fieldNames;

          @Override
          public InputStream getInputStream() {
            connect();
            return input;
          }

          @Override
          public void connect() {
            if (connected) {
              return;
            }
            connected = true;
            var file = files.get(name);
            input = new ByteArrayInputStream(file.content);
            fields = new LinkedHashMap<>();
            initializeHeaders(fields, file);
            fieldNames = new ArrayList<>(fields.keySet());
          }

          private void initializeHeaders(
            Map<String, List<String>> headers,
            FileRecord file
          ) {
            var length = Integer.toString(file.content.length);
            headers.put("content-length", List.of(length));
            var timeStamp = formatTime(file.timestamp);
            headers.put("date", List.of(timeStamp));
            headers.put("last-modified", List.of(timeStamp));
          }

          private String formatTime(long timeStamp) {
            var instant = new Date(timeStamp).toInstant();
            var time = instant.atZone(ZoneId.of("GMT"));
            return DateTimeFormatter.RFC_1123_DATE_TIME.format(time);
          }

          @Override
          public Map<String, List<String>> getHeaderFields() {
            connect();
            return fields;
          }

          @Override
          public String getHeaderField(int index) {
            var name = getHeaderFieldKey(index);
            return name != null ? getHeaderField(name) : null;
          }

          @Override
          public String getHeaderFieldKey(int index) {
            return index < fieldNames.size() ? fieldNames.get(index) : null;
          }

          @Override
          public String getHeaderField(String name) {
            connect();
            return fields.getOrDefault(name, List.of())
              .stream()
              .findFirst()
              .orElse(null);
          }
        };
      }
    }
  }
}