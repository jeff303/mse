package org.apache.spark.sql.catalyst.optimizer

import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.expressions.{AddFields, CreateNamedStruct, GetStructField, Literal, RenameFields}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.rules.RuleExecutor
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}

class SimplifyAddFieldsRenameFieldsTest extends OptimizerTest {

  private object Optimize extends RuleExecutor[LogicalPlan] {
    val batches: Seq[Optimize.Batch] = Batch(
      this.getClass.getSimpleName,
      FixedPoint(50),
      SimplifyAddFieldsRenameFields) :: Nil
  }

  override val Optimizer: RuleExecutor[LogicalPlan] = Optimize

  private val inputStruct = {
    val schema = StructType(Seq(
      StructField("a", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false)))
    val fieldValues = Array(1, 2, 3)
    Literal.create(create_row(fieldValues: _*), schema)
  }

  test("should correctly combine AddFields and RenameFields into CreateNamedStruct, where AddField is being used to add a new field") {
    val newFieldValue = Literal.create(4, IntegerType)
    val expectedExpression = CreateNamedStruct(Seq("x", GetStructField(inputStruct, 0), "b", GetStructField(inputStruct, 1), "b", GetStructField(inputStruct, 2), "c", newFieldValue))
    val expectedEvaluationResult = create_row(1, 2, 3, 4)
    val expectedDataType = StructType(Seq(
      StructField("x", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("c", IntegerType, nullable = false)))

    assertEquivalentPlanAndEvaluation(
      AddFields(RenameFields(inputStruct, "a", "x"), "c", newFieldValue),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)

    assertEquivalentPlanAndEvaluation(
      RenameFields(AddFields(inputStruct, "c", newFieldValue), "a", "x"),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)
  }

  test("should correctly combine AddField and RenameFields into CreateNamedStruct, where AddField is being used to replace an existing field") {
    val newFieldValue = Literal.create(0, IntegerType)
    val expectedExpression = CreateNamedStruct(Seq("x", GetStructField(inputStruct, 0), "b", newFieldValue, "b", newFieldValue))
    val expectedEvaluationResult = create_row(1, 0, 0)
    val expectedDataType = StructType(Seq(
      StructField("x", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false)))

    assertEquivalentPlanAndEvaluation(
      AddFields(RenameFields(inputStruct, "a", "x"), "b", newFieldValue),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)

    assertEquivalentPlanAndEvaluation(
      RenameFields(AddFields(inputStruct, "b", newFieldValue), "a", "x"),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)
  }

  test("should correctly combine AddField and RenameFields into CreateNamedStruct, where RenameFields is being used to rename multiple fields") {
    val newFieldValue = Literal.create(0, IntegerType)
    val expectedExpression = CreateNamedStruct(Seq("a", newFieldValue, "x", GetStructField(inputStruct, 1), "x", GetStructField(inputStruct, 2)))
    val expectedEvaluationResult = create_row(0, 2, 3)
    val expectedDataType = StructType(Seq(
      StructField("a", IntegerType, nullable = false),
      StructField("x", IntegerType, nullable = false),
      StructField("x", IntegerType, nullable = false)))

    assertEquivalentPlanAndEvaluation(
      AddFields(RenameFields(inputStruct, "b", "x"), "a", newFieldValue),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)

    assertEquivalentPlanAndEvaluation(
      RenameFields(AddFields(inputStruct, "a", newFieldValue), "b", "x"),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)
  }

  test("should correctly combine AddField RenameFields DropFields into CreateNamedStruct, where RenameFields is being used to rename a field that was just added") {
    val newFieldValue = Literal.create(4, IntegerType)
    val expectedExpression = CreateNamedStruct(Seq("a", GetStructField(inputStruct, 0), "b", GetStructField(inputStruct, 1), "b", GetStructField(inputStruct, 2), "x", newFieldValue))
    val expectedEvaluationResult = create_row(1, 2, 3, 4)
    val expectedDataType = StructType(Seq(
      StructField("a", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("x", IntegerType, nullable = false)))

    assertEquivalentPlanAndEvaluation(
      RenameFields(AddFields(inputStruct, "c", newFieldValue), "c", "x"),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)
  }

  test("should return null") {
    val nullStruct = Literal.create(null, inputStruct.dataType)
    val newFieldValue = Literal.create(4, IntegerType)
    val expectedExpression = CreateNamedStruct(Seq("a", GetStructField(nullStruct, 0), "b", GetStructField(nullStruct, 1), "b", GetStructField(nullStruct, 2), "x", newFieldValue))
    val expectedEvaluationResult = null
    val expectedDataType = StructType(Seq(
      StructField("a", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("b", IntegerType, nullable = false),
      StructField("x", IntegerType, nullable = false)))

    assertEquivalentPlanAndEvaluation(
      RenameFields(AddFields(nullStruct, "c", newFieldValue), "c", "x"),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)

    assertEquivalentPlanAndEvaluation(
      AddFields(RenameFields(nullStruct, "c", "x"), "c", newFieldValue),
      expectedExpression,
      expectedEvaluationResult,
      expectedDataType)
  }
}
