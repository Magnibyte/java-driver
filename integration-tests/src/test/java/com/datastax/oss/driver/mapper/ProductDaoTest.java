package com.datastax.oss.driver.mapper;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.datastax.oss.driver.internal.mapper.processor.MapperProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class ProductDaoTest {

  private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

  @Test
  public void should_generate_product_dao_impl() throws IOException {
    // given
    String FQCN = "com.datastax.oss.driver.mapper.model.inventory.ProductDao";
    String classLocation =
        "src/test/java/com/datastax/oss/driver/mapper/model/inventory/ProductDao.java";

    // when
    Compilation compilation =
        javac()
            .withProcessors(new MapperProcessor())
            .compile(JavaFileObjects.forSourceLines(FQCN, loadLinesFromSourceFile(classLocation)));

    // then
    assertThat(compilation).succeeded();
    //    assertThat(compilation)
    //        .generatedSourceFile("ProductDao_Impl")
    //        .hasSourceEquivalentTo(JavaFileObjects.forResource("GeneratedHelloWorld.java"));
  }

  private List<String> loadLinesFromSourceFile(String path) throws IOException {
    return Files.lines(Paths.get(path), UTF8_CHARSET).collect(Collectors.toList());
  }
}
