package org.apache.spark.sql.catalyst.optimizer

object SimplifyStructExpressions {
  val rules = Seq(
    // add add
    SimplifyAddFieldsAddFields,
    // rename rename
    SimplifyRenameFieldsRenameFields,
    // drop drop
    SimplifyDropFieldsDropFields,
    // add struct
    SimplifyAddFieldsCreateNamedStruct,
    // rename struct
    SimplifyRenameFieldsCreateNamedStruct,
    // drop struct
    SimplifyDropFieldsCreateNamedStruct,
    // add rename | rename add
    SimplifyAddFieldsRenameFields,
    // drop rename | rename drop
    SimplifyRenameFieldsDropFields,
    // add drop | drop add
    SimplifyAddFieldsDropFields
  )
}