package jsheets.runtime.evaluation.shell.environment.sandbox;

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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;
import jsheets.sandbox.SandboxBytecodeCheck;

public final class SandboxLoader implements LoaderDelegate {
  private final SandboxLoader.RemoteClassLoader loader;
  private final Map<String, Class<?>> types = new HashMap<>();

  public SandboxLoader() {
    this.loader = new RemoteClassLoader();
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
    try {
      for (var binary : binaries) {
        SandboxBytecodeCheck.create().run(binary.bytecodes());
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

  private record ClassFile(byte[] data, long timestamp) {}

  private static class RemoteClassLoader extends URLClassLoader {
    private final Map<String, ClassFile> classFiles = new HashMap<>();

    RemoteClassLoader() {
      super(new URL[0]);
    }

    void declare(String name, byte[] bytes) {
      classFiles.put(toResourceString(name),
        new ClassFile(bytes, System.currentTimeMillis())
      );
    }

    private String toResourceString(String className) {
      return className.replace('.', '/') + ".class";
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      var file = classFiles.get(toResourceString(name));
      if (file == null) {
        return super.findClass(name);
      }
      return super.defineClass(
        name,
        file.data,
        0,
        file.data.length,
        (CodeSource) null
      );
    }

    @Override
    public URL findResource(String name) {
      URL u = doFindResource(name);
      return u != null ? u : super.findResource(name);
    }

    private URL doFindResource(String name) {
      if (classFiles.containsKey(name)) {
        try {
          return new URL(null,
            new URI("jshell", null, "/" + name, null).toString(),
            new RemoteClassLoader.ResourceURLStreamHandler(name)
          );
        } catch (MalformedURLException | URISyntaxException ex) {
          throw new InternalError(ex);
        }
      }

      return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
      URL u = doFindResource(name);
      Enumeration<URL> sup = super.findResources(name);

      if (u == null) {
        return sup;
      }

      List<URL> result = new ArrayList<>();

      while (sup.hasMoreElements()) {
        result.add(sup.nextElement());
      }

      result.add(u);

      return Collections.enumeration(result);
    }

    @Override
    public void addURL(URL url) {
      super.addURL(url);
    }


    private class ResourceURLStreamHandler extends URLStreamHandler {
      private final String name;

      ResourceURLStreamHandler(String name) {
        this.name = name;
      }

      @Override
      protected URLConnection openConnection(URL u) throws IOException {
        return new URLConnection(u) {
          private InputStream in;
          private Map<String, List<String>> fields;
          private List<String> fieldNames;

          @Override
          public InputStream getInputStream() throws IOException {
            connect();
            return in;
          }

          @Override
          public void connect() {
            if (connected) {
              return;
            }
            connected = true;
            var file = classFiles.get(name);
            in = new ByteArrayInputStream(file.data);
            fields = new LinkedHashMap<>();
            fields.put(
              "content-length",
              List.of(Integer.toString(file.data.length))
            );
            Instant instant = new Date(file.timestamp).toInstant();
            ZonedDateTime time = ZonedDateTime.ofInstant(
              instant,
              ZoneId.of("GMT")
            );
            String timeStamp = DateTimeFormatter.RFC_1123_DATE_TIME.format(time);
            fields.put("date", List.of(timeStamp));
            fields.put("last-modified", List.of(timeStamp));
            fieldNames = new ArrayList<>(fields.keySet());
          }

          @Override
          public Map<String, List<String>> getHeaderFields() {
            connect();
            return fields;
          }

          @Override
          public String getHeaderField(int n) {
            String name = getHeaderFieldKey(n);

            return name != null ? getHeaderField(name) : null;
          }

          @Override
          public String getHeaderFieldKey(int n) {
            return n < fieldNames.size() ? fieldNames.get(n) : null;
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