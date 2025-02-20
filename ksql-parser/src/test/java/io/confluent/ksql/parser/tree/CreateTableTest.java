/*
 * Copyright 2019 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.parser.tree;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import io.confluent.ksql.parser.properties.with.CreateSourceProperties;
import io.confluent.ksql.parser.tree.TableElement.Namespace;
import io.confluent.ksql.properties.with.CommonCreateConfigs;
import io.confluent.ksql.schema.ksql.types.SqlTypes;
import java.util.Optional;
import org.junit.Test;

public class CreateTableTest {

  public static final NodeLocation SOME_LOCATION = new NodeLocation(0, 0);
  public static final NodeLocation OTHER_LOCATION = new NodeLocation(1, 0);
  private static final QualifiedName SOME_NAME = QualifiedName.of("bob");
  private static final TableElements SOME_ELEMENTS = TableElements.of(
      new TableElement(Namespace.VALUE, "Bob", new Type(SqlTypes.STRING))
  );
  private static final CreateSourceProperties SOME_PROPS = CreateSourceProperties.from(
      ImmutableMap.of(
      "value_format", new StringLiteral("json"),
      "kafka_topic", new StringLiteral("foo")
      ));
  private static final CreateSourceProperties OTHER_PROPS = CreateSourceProperties.from(
      ImmutableMap.of(
          "value_format", new StringLiteral("json"),
          "kafka_topic", new StringLiteral("foo"),
          CommonCreateConfigs.TIMESTAMP_NAME_PROPERTY, new StringLiteral("foo"))
  );

  @Test
  public void shouldImplementHashCodeAndEqualsProperty() {
    new EqualsTester()
        .addEqualityGroup(
            // Note: At the moment location does not take part in equality testing
            new CreateTable(SOME_NAME, SOME_ELEMENTS, true, SOME_PROPS),
            new CreateTable(SOME_NAME, SOME_ELEMENTS, true, SOME_PROPS),
            new CreateTable(Optional.of(SOME_LOCATION), SOME_NAME, SOME_ELEMENTS, true, SOME_PROPS),
            new CreateTable(Optional.of(OTHER_LOCATION), SOME_NAME, SOME_ELEMENTS, true, SOME_PROPS)
        )
        .addEqualityGroup(
            new CreateTable(QualifiedName.of("jim"), SOME_ELEMENTS, true, SOME_PROPS)
        )
        .addEqualityGroup(
            new CreateTable(SOME_NAME, TableElements.of(), true, SOME_PROPS)
        )
        .addEqualityGroup(
            new CreateTable(SOME_NAME, SOME_ELEMENTS, false, SOME_PROPS)
        )
        .addEqualityGroup(
            new CreateTable(SOME_NAME, SOME_ELEMENTS, true, OTHER_PROPS)
        )
        .testEquals();
  }
}