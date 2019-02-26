/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.driver.mapper;

import static com.datastax.oss.driver.mapper.MapperProcessorTestUtil.loadLinesFromSourceFile;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.datastax.oss.driver.internal.mapper.processor.MapperProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import javax.tools.StandardLocation;
import org.junit.Test;

public class EntityAnnotationTest {

  private static final String PACKAGE_NAME = "com.datastax.oss.driver.mapper.model.inventory";

  @Test
  public void should_generate_product_helper_based_on_entity_annotation() throws IOException {
    // given
    String sourceFileLocation =
        "src/test/java/com/datastax/oss/driver/mapper/model/inventory/Product.java";
    String generatedSourceFileLocation =
        this.getClass().getResource("/annotation-processor/Product_Helper_Expected").getPath();

    // when
    Compilation compilation =
        javac()
            .withProcessors(new MapperProcessor())
            .compile(
                JavaFileObjects.forSourceLines(
                    PACKAGE_NAME + ".Product", loadLinesFromSourceFile(sourceFileLocation)));

    // then
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
        .generatedFile(StandardLocation.SOURCE_OUTPUT, PACKAGE_NAME, "Product_Helper.java")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceLines(
                PACKAGE_NAME + "Product_Helper_Expected",
                loadLinesFromSourceFile(generatedSourceFileLocation)));
  }

  @Test
  public void should_generate_dimensions_helper_based_on_entity_annotation() throws IOException {
    // given
    String sourceFileLocation =
        "src/test/java/com/datastax/oss/driver/mapper/model/inventory/Dimensions.java";
    String generatedSourceFileLocation =
        this.getClass().getResource("/annotation-processor/Dimensions_Helper_Expected").getPath();

    // when
    Compilation compilation =
        javac()
            .withProcessors(new MapperProcessor())
            .compile(
                JavaFileObjects.forSourceLines(
                    PACKAGE_NAME + ".Dimensions", loadLinesFromSourceFile(sourceFileLocation)));

    // then
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
        .generatedFile(StandardLocation.SOURCE_OUTPUT, PACKAGE_NAME, "Dimensions_Helper.java")
        .hasSourceEquivalentTo(
            JavaFileObjects.forSourceLines(
                PACKAGE_NAME + "Dimensions_Helper_Expected",
                loadLinesFromSourceFile(generatedSourceFileLocation)));
  }
}
