/*
 * This source file is part fo the SWAN open-source project.
 *
 * Copyright (c) 2020 the SWAN project authors.
 * Licensed under Apache License v2.0
 *
 * See https://github.com/themaplelab/swan/LICENSE.txt for license information.
 *
 */

/* NOTES
 *  - Results are not in OperatorDef because some operators do not
 *    have a result. Also, it should be explicit whether an instruction
 *    has a result.
 */

package ca.ualberta.maple.swan.ir

import ca.ualberta.maple.swan.parser.{SILBlock, SILFunction, SILModule}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

// NOTE: 'var' is used where passes mutate date

// Imports might be useful for modelling later, but probably not.
class Module(val functions: ArrayBuffer[Function], val imports: Array[String])

class Function(val attribute: Option[FunctionAttribute], val name: String, val tpe: Type,
               var blocks: ArrayBuffer[Block], val refTable: RefTable)

class Block(val blockRef: BlockRef, var arguments: Array[Argument],
            var operators: ArrayBuffer[OperatorDef], var terminator: TerminatorDef)

sealed trait FunctionAttribute
object FunctionAttribute {
  case object global_init extends FunctionAttribute
  case object coroutine extends FunctionAttribute
  case object stub extends FunctionAttribute
  case object model extends FunctionAttribute
}

// Only String for now
class Type(val name: String = "Any")

class Position(val path: String, val line: Int, val col: Int)

class Argument(val name: SymbolRef, val tpe: Type)

sealed abstract trait InstructionDef {
  val instruction: Instruction
}
object InstructionDef {
  case class operator(operatorDef: OperatorDef) extends InstructionDef {
    override val instruction: Instruction = Instruction.operator(operatorDef.operator)
  }
  case class terminator(terminatorDef: TerminatorDef) extends InstructionDef {
    override val instruction: Instruction = Instruction.terminator(terminatorDef.terminator)
  }
}

class OperatorDef(val operator: Operator, val position: Option[Position])

class TerminatorDef(val terminator: Terminator, val position: Option[Position])

sealed trait Instruction
object Instruction {
  case class operator(op: Operator) extends Instruction
  case class terminator(t: Terminator) extends Instruction
}

sealed trait Operator
object Operator { // WIP
  // *** SHARED ***
  case class newGlobal(name: String) extends Operator
  case class neww(result: Symbol) extends WithResult(result)
  case class assignGlobal(result: Symbol, name: String) extends WithResult(result)
  case class assign(result: Symbol, from: SymbolRef) extends WithResult(result)
  case class literal(result: Symbol, literal: Literal) extends WithResult(result)
  case class builtinRef(result: Symbol, decl: Boolean, name: String) extends WithResult(result)
  case class functionRef(result: Symbol, var names: Array[String]) extends WithResult(result)
  case class apply(result: Symbol, functionRef: SymbolRef, arguments: Array[SymbolRef]) extends WithResult(result)
  case class arrayRead(result: Symbol, alias: Boolean, arr: SymbolRef) extends WithResult(result)
  case class arrayWrite(value: SymbolRef, arr: SymbolRef) extends Operator
  case class fieldRead(result: Symbol, alias: Boolean, obj: SymbolRef, field: String) extends WithResult(result)
  case class fieldWrite(value: SymbolRef, obj: SymbolRef, field: String) extends Operator
  case class unaryOp(result: Symbol, operation: UnaryOperation, operand: SymbolRef) extends WithResult(result)
  case class binaryOp(result: Symbol, operation: BinaryOperation, lhs: SymbolRef, rhs: SymbolRef) extends WithResult(result)
  case class condFail(value: SymbolRef) extends Operator
  case class switchEnumAssign(result: Symbol, switchOn: SymbolRef,
                              cases: Array[EnumAssignCase], default: Option[SymbolRef]) extends WithResult(result)
  // *** RAW ONLY ***
  case class applyCoroutine(functionRef: SymbolRef, arguments: Array[SymbolRef]) extends Operator
  case class pointerRead(result: Symbol, pointer: SymbolRef) extends WithResult(result)
  case class pointerWrite(value: SymbolRef, pointer: SymbolRef) extends Operator
  case class abortCoroutine(value: SymbolRef) extends Operator
  case class endCoroutine(value: SymbolRef) extends Operator

  // *** CANONICAL ***
  case class unhandledApply(result: Symbol, function: String, arguments: Array[SymbolRef]) extends WithResult(result)
}

abstract class WithResult(val value: Symbol) extends Operator

sealed trait Terminator
object Terminator {
  case class br(to: BlockRef, args: Array[SymbolRef]) extends Terminator
  case class condBr(cond: SymbolRef, trueBlock: BlockRef, trueArgs: Array[SymbolRef],
                    falseBlock: BlockRef, falseArgs: Array[SymbolRef]) extends Terminator
  case class switch(switchOn: SymbolRef, cases: Array[SwitchCase], default: Option[BlockRef]) extends Terminator
  case class switchEnum(switchOn: SymbolRef, cases: Array[SwitchEnumCase], default: Option[BlockRef]) extends Terminator
  case class ret(value: SymbolRef) extends Terminator
  case class thro(value: SymbolRef) extends Terminator
  case class tryApply(functionRef: SymbolRef, arguments: Array[SymbolRef],
                      normal: BlockRef, error: BlockRef) extends Terminator
  // *** RAW ONLY ***
  case object unreachable extends Terminator
  case class yld(yields: Array[SymbolRef], resume: BlockRef, unwind: BlockRef) extends Terminator
  case object unwind extends Terminator
}

// swanir-enum-assign-case ::= 'case' sil-decl-ref ':' swanir-value-name
class EnumAssignCase(val decl: String, val value: SymbolRef)

// swanir-switch-case ::= 'case' swanir-value-name ':' swanir-identifier
class SwitchCase(val value: String, val destination: BlockRef)

// swanir-switch-enum-case ::= 'case' sil-decl-ref ':' swanir-value-name
class SwitchEnumCase(val decl: String, val destination: BlockRef)

sealed trait UnaryOperation
object UnaryOperation {
  case object arbitrary extends UnaryOperation
}

sealed trait BinaryOperation
object BinaryOperation {
  case object arbitrary extends BinaryOperation
}

sealed trait Literal
object Literal {
  case class string(value: String) extends Literal
  case class int(value: BigInt) extends Literal
  case class float(value: Float) extends Literal
}

class Symbol(val ref: SymbolRef, val tpe: Type)

// This is so that we can change symbol names throughout the program
// for things like symbol_copy folding.
class SymbolRef(var name: String)
class BlockRef(var label: String) // Might need it
// No function ref for now (I don't see a reason for it)

class RefTable {
  val symbols = new mutable.HashMap[String, SymbolRef]()
  val blocks = new mutable.HashMap[String, BlockRef]()
}

class SymbolTables {
  // Map of function to its value table
  val tables = new mutable.HashMap[String, mutable.HashMap[String, SymbolTableEntry]]()
  // TODO: global table
}

sealed trait SymbolTableEntry
object SymbolTableEntry {
  case class operator(symbol: Symbol, var operator: Operator) extends SymbolTableEntry
  case class argument(argument: Argument) extends SymbolTableEntry
}

// This isn't true dynamic context. It's just a container to hold
// the module/function/block when translating instructions.
class Context(val silModule: SILModule, val silFunction: SILFunction,
              val silBlock: SILBlock, val pos: Option[Position],
              val refTable: RefTable)
