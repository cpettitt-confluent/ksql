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

package io.confluent.ksql.parser.rewrite;

import io.confluent.ksql.parser.tree.AliasedRelation;
import io.confluent.ksql.parser.tree.AllColumns;
import io.confluent.ksql.parser.tree.ArithmeticBinaryExpression;
import io.confluent.ksql.parser.tree.ArithmeticUnaryExpression;
import io.confluent.ksql.parser.tree.AstNode;
import io.confluent.ksql.parser.tree.BetweenPredicate;
import io.confluent.ksql.parser.tree.Cast;
import io.confluent.ksql.parser.tree.ComparisonExpression;
import io.confluent.ksql.parser.tree.CreateStream;
import io.confluent.ksql.parser.tree.CreateStreamAsSelect;
import io.confluent.ksql.parser.tree.CreateTable;
import io.confluent.ksql.parser.tree.CreateTableAsSelect;
import io.confluent.ksql.parser.tree.DefaultAstVisitor;
import io.confluent.ksql.parser.tree.DereferenceExpression;
import io.confluent.ksql.parser.tree.DropTable;
import io.confluent.ksql.parser.tree.Expression;
import io.confluent.ksql.parser.tree.FunctionCall;
import io.confluent.ksql.parser.tree.GroupBy;
import io.confluent.ksql.parser.tree.GroupingElement;
import io.confluent.ksql.parser.tree.InListExpression;
import io.confluent.ksql.parser.tree.InPredicate;
import io.confluent.ksql.parser.tree.InsertInto;
import io.confluent.ksql.parser.tree.IsNotNullPredicate;
import io.confluent.ksql.parser.tree.IsNullPredicate;
import io.confluent.ksql.parser.tree.Join;
import io.confluent.ksql.parser.tree.KsqlWindowExpression;
import io.confluent.ksql.parser.tree.LikePredicate;
import io.confluent.ksql.parser.tree.Literal;
import io.confluent.ksql.parser.tree.LogicalBinaryExpression;
import io.confluent.ksql.parser.tree.Node;
import io.confluent.ksql.parser.tree.NotExpression;
import io.confluent.ksql.parser.tree.QualifiedNameReference;
import io.confluent.ksql.parser.tree.Query;
import io.confluent.ksql.parser.tree.Relation;
import io.confluent.ksql.parser.tree.SearchedCaseExpression;
import io.confluent.ksql.parser.tree.Select;
import io.confluent.ksql.parser.tree.SelectItem;
import io.confluent.ksql.parser.tree.SimpleCaseExpression;
import io.confluent.ksql.parser.tree.SimpleGroupBy;
import io.confluent.ksql.parser.tree.SingleColumn;
import io.confluent.ksql.parser.tree.Statement;
import io.confluent.ksql.parser.tree.Statements;
import io.confluent.ksql.parser.tree.SubscriptExpression;
import io.confluent.ksql.parser.tree.Table;
import io.confluent.ksql.parser.tree.TableElement;
import io.confluent.ksql.parser.tree.TableElements;
import io.confluent.ksql.parser.tree.WhenClause;
import io.confluent.ksql.parser.tree.WindowExpression;
import io.confluent.ksql.parser.tree.WithinExpression;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * This class will create a new AST given the input AST. The new AST is exactly the clone of
 * the input one. If you want to rewrite a query by changing the AST you can inherit from this
 * class and implemet the changes for the nodes you need. The newly generated tree will include
 * your changes and the rest of the tree will remain the same.
 *
 * <p>Implementation note: make sure if you change this class you create each node only once in the
 * if/else blocks.
 * </p>
 */
// CHECKSTYLE_RULES.OFF: ClassDataAbstractionCoupling
public class StatementRewriter extends DefaultAstVisitor<Node, Object> {
  // CHECKSTYLE_RULES.ON: ClassDataAbstractionCoupling

  protected Expression visitExpression(final Expression node, final Object context) {
    return node;
  }

  public Expression visitArithmeticBinary(
      final ArithmeticBinaryExpression node,
      final Object context
  ) {
    final Expression rewrittenLeft = (Expression) process(node.getLeft(), context);
    final Expression rewrittenRight = (Expression) process(node.getRight(), context);

    return new ArithmeticBinaryExpression(
        node.getLocation(),
        node.getOperator(),
        rewrittenLeft,
        rewrittenRight);
  }

  public Expression visitBetweenPredicate(final BetweenPredicate node, final Object context) {
    return new BetweenPredicate(
        node.getLocation(),
        (Expression) process(node.getValue(), context),
        (Expression) process(node.getMin(), context),
        (Expression) process(node.getMax(), context));
  }

  public Expression visitComparisonExpression(
      final ComparisonExpression node,
      final Object context) {
    return new ComparisonExpression(
        node.getLocation(),
        node.getType(),
        (Expression) process(node.getLeft(), context),
        (Expression) process(node.getRight(), context));
  }

  protected AstNode visitStatements(final Statements node, final Object context) {
    final List<Statement> rewrittenStatements = node.getStatements()
        .stream()
        .map(s -> (Statement) process(s, context))
        .collect(Collectors.toList());

    return new Statements(
        node.getLocation(),
        rewrittenStatements
    );
  }

  protected Query visitQuery(final Query node, final Object context) {
    final Select select = (Select) process(node.getSelect(), context);

    final Relation from = (Relation) process(node.getFrom(), context);

    final Optional<WindowExpression> windowExpression = node.getWindow()
        .map(exp -> ((WindowExpression) process(exp, context)));

    final Optional<Expression> where = node.getWhere()
        .map(exp -> ((Expression) process(exp, context)));

    final Optional<GroupBy> groupBy = node.getGroupBy()
        .map(exp -> ((GroupBy) process(exp, context)));

    final Optional<Expression> having = node.getHaving()
        .map(exp -> ((Expression) process(exp, context)));

    return new Query(
        node.getLocation(),
        select,
        from,
        windowExpression,
        where,
        groupBy,
        having,
        node.getLimit()
    );
  }

  protected AstNode visitSelect(final Select node, final Object context) {
    final List<SelectItem> rewrittenItems = node.getSelectItems()
        .stream()
        .map(selectItem -> (SelectItem) process(selectItem, context))
        .collect(Collectors.toList());

    return new Select(
        node.getLocation(),
        rewrittenItems
    );
  }

  public Expression visitWhenClause(final WhenClause node, final Object context) {
    return new WhenClause(
        node.getLocation(),
        (Expression) process(node.getOperand(), context),
        (Expression) process(node.getResult(), context)
    );
  }

  public Expression visitInPredicate(final InPredicate node, final Object context) {
    return new InPredicate(
        node.getLocation(),
        (Expression) process(node.getValue(), context),
        (InListExpression) process(node.getValueList(), context)
    );
  }

  public Expression visitFunctionCall(final FunctionCall node, final Object context) {
    final List<Expression> rewrittenArgs = node.getArguments()
        .stream()
        .map(arg -> (Expression) process(arg, context))
        .collect(Collectors.toList());

    return new FunctionCall(
        node.getLocation(),
        node.getName(),
        rewrittenArgs
    );
  }

  public Expression visitSimpleCaseExpression(
      final SimpleCaseExpression node,
      final Object context) {
    final Expression operand = (Expression) process(node.getOperand(), context);

    final List<WhenClause> when = node.getWhenClauses().stream()
        .map(exp -> (WhenClause) process(exp, context))
        .collect(Collectors.toList());

    final Optional<Expression> defaultValue = node.getDefaultValue()
        .map(exp -> ((Expression) process(exp, context)));

    return new SimpleCaseExpression(
        node.getLocation(),
        operand,
        when,
        defaultValue
      );
  }

  public Expression visitInListExpression(final InListExpression node, final Object context) {
    final List<Expression> rewrittenExpressions = node.getValues().stream()
        .map(value -> (Expression) process(value, context))
        .collect(Collectors.toList());

    return new InListExpression(node.getLocation(), rewrittenExpressions);
  }

  public Expression visitQualifiedNameReference(
      final QualifiedNameReference node,
      final Object context
  ) {
    return node;
  }

  public Expression visitDereferenceExpression(
      final DereferenceExpression node,
      final Object context
  ) {
    return new DereferenceExpression(
        node.getLocation(),
        (Expression) process(node.getBase(), context),
        node.getFieldName()
    );
  }

  public Expression visitLiteral(final Literal node, final Object context) {
    return node;
  }

  public Expression visitArithmeticUnary(
      final ArithmeticUnaryExpression node,
      final Object context) {
    final Expression rewrittenExpression = (Expression) process(node.getValue(), context);

    return new ArithmeticUnaryExpression(
        node.getLocation(),
        node.getSign(),
        rewrittenExpression
    );
  }

  public Expression visitNotExpression(final NotExpression node, final Object context) {
    return new NotExpression(
        node.getLocation(),
        (Expression) process(node.getValue(), context)
    );
  }

  protected AstNode visitSingleColumn(final SingleColumn node, final Object context) {
    return node.copyWithExpression((Expression) process(node.getExpression(), context));
  }

  protected AstNode visitAllColumns(final AllColumns node, final Object context) {
    return node;
  }

  public Expression visitSearchedCaseExpression(
      final SearchedCaseExpression node,
      final Object context
  ) {
    final List<WhenClause> whenClauses = node.getWhenClauses()
        .stream().map(whenClause -> (WhenClause) process(whenClause, context))
        .collect(Collectors.toList());

    final Optional<Expression> defaultValue = node.getDefaultValue()
        .map(exp -> ((Expression) process(exp, context)));

    return new SearchedCaseExpression(
        node.getLocation(),
        whenClauses,
        defaultValue
    );
  }

  public Expression visitLikePredicate(final LikePredicate node, final Object context) {
    return new LikePredicate(
        node.getLocation(),
        (Expression) process(node.getValue(), context),
        (Expression) process(node.getPattern(), context)
      );
  }

  public Expression visitIsNotNullPredicate(final IsNotNullPredicate node, final Object context) {
    return new IsNotNullPredicate(
        node.getLocation(),
        (Expression) process(node.getValue(), context)
    );
  }

  public Expression visitIsNullPredicate(final IsNullPredicate node, final Object context) {
    return new IsNullPredicate(
        node.getLocation(),
        (Expression) process(node.getValue(), context)
    );
  }

  public Expression visitSubscriptExpression(
      final SubscriptExpression node,
      final Object context) {
    return new SubscriptExpression(
        node.getLocation(),
        (Expression) process(node.getBase(), context),
        (Expression) process(node.getIndex(), context)
    );
  }

  public Expression visitLogicalBinaryExpression(
      final LogicalBinaryExpression node,
      final Object context
  ) {
    return new LogicalBinaryExpression(
        node.getLocation(),
        node.getType(),
        (Expression) process(node.getLeft(), context),
        (Expression) process(node.getRight(), context)
    );
  }

  protected AstNode visitTable(final Table node, final Object context) {
    return node;
  }

  protected AstNode visitAliasedRelation(final AliasedRelation node, final Object context) {
    final Relation rewrittenRelation = (Relation) process(node.getRelation(), context);

    return new AliasedRelation(
        node.getLocation(),
        rewrittenRelation,
        node.getAlias());
  }

  protected AstNode visitJoin(final Join node, final Object context) {
    final Relation rewrittenLeft = (Relation) process(node.getLeft(), context);
    final Relation rewrittenRight = (Relation) process(node.getRight(), context);
    final Optional<WithinExpression> rewrittenWithin = node.getWithinExpression()
        .map(within -> (WithinExpression) process(within, context));

    return new Join(
        node.getLocation(),
        node.getType(),
        rewrittenLeft,
        rewrittenRight,
        node.getCriteria(),
        rewrittenWithin);
  }

  @Override
  protected AstNode visitWithinExpression(final WithinExpression node, final Object context) {
    return node;
  }

  public Expression visitCast(final Cast node, final Object context) {
    final Expression expression = (Expression) process(node.getExpression(), context);
    return new Cast(node.getLocation(), expression, node.getType());
  }

  protected AstNode visitWindowExpression(final WindowExpression node, final Object context) {
    return new WindowExpression(
          node.getLocation(),
          node.getWindowName(),
          (KsqlWindowExpression) process(node.getKsqlWindowExpression(), context));
  }

  protected AstNode visitKsqlWindowExpression(
      final KsqlWindowExpression node,
      final Object context) {
    return node;
  }

  protected AstNode visitTableElement(final TableElement node, final Object context) {
    return node;
  }

  protected AstNode visitCreateStream(final CreateStream node, final Object context) {
    final List<TableElement> rewrittenElements = node.getElements().stream()
        .map(tableElement -> (TableElement) process(tableElement, context))
        .collect(Collectors.toList());

    return node.copyWith(TableElements.of(rewrittenElements), node.getProperties());
  }

  protected AstNode visitCreateStreamAsSelect(
      final CreateStreamAsSelect node,
      final Object context) {
    final Optional<Expression> partitionBy = node.getPartitionByColumn()
        .map(exp -> (Expression) process(exp,context));

    return new CreateStreamAsSelect(
        node.getLocation(),
        node.getName(),
        (Query) process(node.getQuery(), context),
        node.isNotExists(),
        node.getProperties(),
        partitionBy
    );
  }

  protected AstNode visitCreateTable(final CreateTable node, final Object context) {
    final List<TableElement> rewrittenElements = node.getElements().stream()
        .map(tableElement -> (TableElement) process(tableElement, context))
        .collect(Collectors.toList());

    return node.copyWith(TableElements.of(rewrittenElements), node.getProperties());
  }

  protected AstNode visitCreateTableAsSelect(
      final CreateTableAsSelect node,
      final Object context) {
    return new CreateTableAsSelect(
        node.getLocation(),
        node.getName(),
        (Query) process(node.getQuery(), context),
        node.isNotExists(),
        node.getProperties()
    );
  }

  protected AstNode visitInsertInto(final InsertInto node, final Object context) {
    final Optional<Expression> rewrittenPartitionBy = node.getPartitionByColumn()
        .map(exp -> (Expression) process(exp, context));

    return new InsertInto(
        node.getLocation(),
        node.getTarget(),
        (Query) process(node.getQuery(), context),
        rewrittenPartitionBy);
  }

  protected AstNode visitDropTable(final DropTable node, final Object context) {
    return node;
  }

  protected AstNode visitGroupBy(final GroupBy node, final Object context) {
    final List<GroupingElement> rewrittenGroupings = node.getGroupingElements().stream()
        .map(groupingElement -> (GroupingElement) process(groupingElement, context))
        .collect(Collectors.toList());

    return new GroupBy(node.getLocation(), rewrittenGroupings);
  }

  protected AstNode visitSimpleGroupBy(final SimpleGroupBy node, final Object context) {
    final List<Expression> columns = node.getColumns().stream()
        .map(ce -> (Expression) process(ce, context))
        .collect(Collectors.toList());

    return new SimpleGroupBy(
        node.getLocation(),
        columns
      );
  }
}