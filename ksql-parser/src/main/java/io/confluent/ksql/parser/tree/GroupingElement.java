/*
 * Copyright 2018 Confluent Inc.
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

import com.google.errorprone.annotations.Immutable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Immutable
public abstract class GroupingElement extends AstNode {

  GroupingElement(final Optional<NodeLocation> location) {
    super(location);
  }

  public abstract List<Set<Expression>> enumerateGroupingSets();

  @Override
  protected <R, C> R accept(final AstVisitor<R, C> visitor, final C context) {
    return visitor.visitGroupingElement(this, context);
  }

  public abstract String format();
}
