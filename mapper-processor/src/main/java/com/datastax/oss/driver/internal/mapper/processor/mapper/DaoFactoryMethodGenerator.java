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
package com.datastax.oss.driver.internal.mapper.processor.mapper;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.mapper.annotations.DaoKeyspace;
import com.datastax.oss.driver.api.mapper.annotations.DaoTable;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;
import com.datastax.oss.driver.internal.mapper.DaoCacheKey;
import com.datastax.oss.driver.internal.mapper.processor.MethodGenerator;
import com.datastax.oss.driver.internal.mapper.processor.ProcessorContext;
import com.datastax.oss.driver.internal.mapper.processor.SkipGenerationException;
import com.datastax.oss.driver.internal.mapper.processor.util.generation.GeneratedCodePatterns;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Generates the implementation of a DAO-producing method in a {@link Mapper}-annotated interface.
 */
public class DaoFactoryMethodGenerator implements MethodGenerator {

  private final ExecutableElement methodElement;
  private final CharSequence keyspaceArgumentName;
  private final CharSequence tableArgumentName;
  private final ClassName daoImplementationName;
  private final boolean isAsync;
  private final MapperImplementationSharedCode enclosingClass;
  private final boolean isCachedByKeyspaceAndTable;

  public DaoFactoryMethodGenerator(
      ExecutableElement methodElement,
      ClassName daoImplementationName,
      boolean isAsync,
      MapperImplementationSharedCode enclosingClass,
      ProcessorContext context) {
    this.methodElement = methodElement;
    this.daoImplementationName = daoImplementationName;
    this.isAsync = isAsync;
    this.enclosingClass = enclosingClass;

    VariableElement tmpKeyspace = null;
    VariableElement tmpTable = null;
    for (VariableElement parameterElement : methodElement.getParameters()) {
      if (parameterElement.getAnnotation(DaoKeyspace.class) != null) {
        tmpKeyspace = validateParameter(parameterElement, tmpKeyspace, DaoKeyspace.class, context);
      } else if (parameterElement.getAnnotation(DaoTable.class) != null) {
        tmpTable = validateParameter(parameterElement, tmpTable, DaoTable.class, context);
      } else {
        context
            .getMessager()
            .error(
                methodElement,
                "Only parameters annotated with @%s or @%s are allowed",
                DaoKeyspace.class.getSimpleName(),
                DaoTable.class.getSimpleName());
        throw new SkipGenerationException();
      }
    }
    isCachedByKeyspaceAndTable = (tmpKeyspace != null || tmpTable != null);
    keyspaceArgumentName = (tmpKeyspace == null) ? null : tmpKeyspace.getSimpleName();
    tableArgumentName = (tmpTable == null) ? null : tmpTable.getSimpleName();
  }

  private VariableElement validateParameter(
      VariableElement candidate,
      VariableElement previous,
      Class<?> annotation,
      ProcessorContext context) {
    if (previous != null) {
      context
          .getMessager()
          .error(
              candidate,
              "Only one parameter can be annotated with @%s",
              annotation.getSimpleName());
      throw new SkipGenerationException();
    }
    TypeMirror type = candidate.asType();
    if (!context.getClassUtils().isSame(type, String.class)
        && !context.getClassUtils().isSame(type, CqlIdentifier.class)) {
      context
          .getMessager()
          .error(
              candidate,
              "@%s-annotated parameter must be of type %s or %s",
              annotation.getSimpleName(),
              String.class.getSimpleName(),
              CqlIdentifier.class.getSimpleName());
      throw new SkipGenerationException();
    }
    return candidate;
  }

  @Override
  public MethodSpec.Builder generate() {
    TypeName returnTypeName = ClassName.get(methodElement.getReturnType());
    String suggestedFieldName = methodElement.getSimpleName() + "Cache";
    String fieldName =
        isCachedByKeyspaceAndTable
            ? enclosingClass.addDaoMapField(suggestedFieldName, returnTypeName)
            : enclosingClass.addDaoSimpleField(
                suggestedFieldName, returnTypeName, daoImplementationName, isAsync);

    MethodSpec.Builder overridingMethodBuilder = GeneratedCodePatterns.override(methodElement);

    if (isCachedByKeyspaceAndTable) {
      // DaoCacheKey key = new DaoCacheKey(x, y)
      // where x, y is either the name of the parameter or "(CqlIdentifier)null"
      overridingMethodBuilder.addCode("$1T key = new $1T(", DaoCacheKey.class);
      if (keyspaceArgumentName == null) {
        overridingMethodBuilder.addCode("($T)null", CqlIdentifier.class);
      } else {
        overridingMethodBuilder.addCode("$L", keyspaceArgumentName);
      }
      overridingMethodBuilder.addCode(", ");
      if (tableArgumentName == null) {
        overridingMethodBuilder.addCode("($T)null", CqlIdentifier.class);
      } else {
        overridingMethodBuilder.addCode("$L", tableArgumentName);
      }
      overridingMethodBuilder
          .addCode(");\n")
          .addStatement(
              "return $L.computeIfAbsent(key, "
                  + "k -> $T.$L(context.withKeyspaceAndTable(k.getKeyspaceId(), k.getTableId())))",
              fieldName,
              daoImplementationName,
              isAsync ? "initAsync" : "init");
    } else {
      overridingMethodBuilder.addStatement("return $L", fieldName);
    }
    return overridingMethodBuilder;
  }
}