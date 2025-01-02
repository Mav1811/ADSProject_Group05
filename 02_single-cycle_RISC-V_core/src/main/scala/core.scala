// ADS I Class Project
// Single-Cycle RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 05/10/2023 by Tobias Jauch (@tojauch)

/*
The goal of this task is to implement a simple 32-bit RISC-V processor supporting parts of the RV32I instruction set architecture. This RV32I core is relatively basic
and does not include features like memory operations, exception handling, or branch instructions. It is designed for a simplified subset of the RISC-V ISA. It mainly
focuses on ALU operations and basic instruction execution.

    Instruction Memory:
        The code initializes an instruction memory "mem" with a capacity of 4096 32-bit words.
        It loads the content of this memory from a binary file specified by the BinaryFile parameter.

    CPU Registers:
        The code initializes a program counter (PC) register "PC" with an initial value of 0.
        The core contains a register file "regFile" with 32 entries. Register x0 is hard-wired to always contain the value 0.

    Fetch Stage:
        The Fetch Stage reads the current instruction from "mem" based on the value of the program counter PC.

    Decode Stage:
        This stage extracts various fields (opcode, rd, funct3, rs1, funct7, and rs2) from the fetched instruction, according to the RISC-V ISA specification.
        The core determines the operands for ALU operations based on the instruction type and stores them into two variables "operandA" and "operandB".
        For R-Type instructions, both operands are read from regFile. For I-Type instructions, operandA is read from regFile, while operandB is an
        immediate value, encoded in the instruction.

        The code defines a set of boolean signals (isADD, isADDI, etc.) to identify the type of instruction based on the opcode and function fields. These
        signals are then used to determine the operation to be performed in the Execute Stage.

    Execute Stage:
        In the Execute Stage, ALU operations are performed based on the instruction type and the operands. The result is stored in "aluResult".

    Memory Stage:
        This core does not include memory operations, leave this section empty.

    Write Back Stage:
        The result of the ALU operation ("aluResult") is propagated to "writeBackData".
        The code writes the value contained ba "writeBackData" to the destination register in the regFile specified by "rd".

    Check Result Signal:
        The result of the ALU operation is also provided as output to "io.check_res". This signal will be used by our test cases to validate the actions performed by our core.

    Update PC:
        The program counter PC is incremented by 4 in every cycle, as this core does not include jumps or branches. It simply fetches instructions sequentially.
*/

package core_tile

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class RV32Icore (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res = Output(UInt(32.W))
  })


  // -----------------------------------------
  // Instruction Memory
  // -----------------------------------------

  val IMem = Mem(4096, UInt(32.W))
  loadMemoryFromFile(IMem, BinaryFile)

  // -----------------------------------------
  // CPU Registers
  // -----------------------------------------

  /*
   * TODO: Implement the program counter as a register, initialize with zero
   */
  val pc = RegInit(0.U(12.W))

  val regFile = Mem(32, UInt(32.W))
  /*val regAdr_1 = Wire(UInt(5.W))
  val regWriteEn = Wire(UInt(1.W))
  val regAdr_2 = Wire(UInt(5.W))
  val regA = RegInit(0.U(32.W))
  val regB = RegInit(0.U(32.W))
  val writeAddr = Wire(UInt(5.W))
  val writeValue = Wire(UInt(5.W))
  /*
   * TODO: hard-wire register x0 to zero
   */*/
  regFile(0) := 0.U

  // -----------------------------------------
  // Fetch
  // -----------------------------------------

  val instr  = Wire(UInt(32.W))
  instr := IMem(pc>>2.U)

  // -----------------------------------------
  // Decode
  // -----------------------------------------

  val opcode = instr(6, 0)
  val funct3 = instr(14, 12)
  val funct7 = instr(31, 25)
  val shifttype = instr(30)
  val shamt = instr(24, 20)
  val rd = instr(11, 7)

  /*
   * TODO: Add missing fields from fetched instructions for decoding
   */

  val isADD  = (opcode === "b0110011".U && funct3 === "b000".U && funct7 === "b0000000".U)
  val isSUB  = (opcode === "b0110011".U && funct3 === "b000".U && funct7 === "b0100000".U)
  val isAND  = (opcode === "b0110011".U && funct3 === "b111".U && funct7 === "b0000000".U)
  val isOR   = (opcode === "b0110011".U && funct3 === "b110".U && funct7 === "b0000000".U)
  val isXOR  = (opcode === "b0110011".U && funct3 === "b100".U && funct7 === "b0000000".U)
  val isSLTU  = (opcode === "b0110011".U && funct3 === "b011".U && funct7 === "b0000000".U)
  val isSLT  = (opcode === "b0110011".U && funct3 === "b010".U && funct7 === "b0000000".U)
  val isSLL  = (opcode === "b0110011".U && funct3 === "b001".U && funct7 === "b0000000".U)
  val isSRL  = (opcode === "b0110011".U && funct3 === "b101".U && funct7 === "b0000000".U)
  val isSRA  = (opcode === "b0110011".U && funct3 === "b101".U && funct7 === "b0100000".U)

  /*
   * TODO: Add missing R-Type instructions here
   */


  val isADDI = (opcode === "b0010011".U && funct3 === "b000".U)
  val isSLTI = (opcode === "b0010011".U && funct3 === "b010".U)
  val isSLTIU = (opcode === "b0010011".U && funct3 === "b011".U)
  val isANDI = (opcode === "b0010011".U && funct3 === "b111".U)
  val isORI = (opcode === "b0010011".U && funct3 === "b110".U)
  val isXORI = (opcode === "b0010011".U && funct3 === "b100".U)
  val isSLLI = (opcode === "b0010011".U && funct3 === "b001".U)
  val isSRLI = (opcode === "b0010011".U && funct3 === "b101".U && shifttype === "b0".U)
  val isSRAI = (opcode === "b0010011".U && funct3 === "b001".U && shifttype === "b1".U)


  // Operands

   /*
   * TODO: Add operand signals accoring to specification
   */
  val operandA = regFile(instr(19, 15)).asSInt
  val operandB = Wire(SInt(32.W))
  operandB := 0.S
  when( opcode === "b0010011".U){
    val operandB = instr(31, 20).asSInt
  }.elsewhen(opcode === "b0110011".U){
    val operandB = regFile(instr(24, 20)).asSInt
  }


  // -----------------------------------------
  // Execute
  // -----------------------------------------

  val aluResult = Wire(SInt(32.W))

  when(isADDI) { // start of I type
    aluResult := operandA + operandB
  }.elsewhen(isSLTI) {
    aluResult := (operandA  < operandB).asSInt
  }.elsewhen(isSLTIU) {
    aluResult := (operandA.asUInt < (operandB).asUInt).asSInt
  }.elsewhen(isANDI) {
    aluResult := operandA & operandB
  }.elsewhen(isORI) {
    aluResult := operandA | operandB
  }.elsewhen(isXORI) {
    aluResult := operandA ^ operandB
  }.elsewhen(isSLLI) {
    aluResult := operandA << shamt
  }.elsewhen(isSRLI) {
    aluResult := (operandA >> shamt)
  }.elsewhen(isSRAI) {
    aluResult := (operandA >> shamt) //end of I Type
  }.elsewhen(isADD) { // start of R type
    aluResult := operandA + operandB
  }.elsewhen(isSUB) {
    aluResult := operandA - operandB
  }.elsewhen(isAND) {
    aluResult := operandA & operandB
  }.elsewhen(isOR) {
    aluResult := operandA | operandB
  }.elsewhen(isXOR) {
    aluResult := operandA ^ operandB
  }.elsewhen(isSLT) {
    aluResult := (operandA < operandB).asSInt
  }.elsewhen(isSLTU) {
    aluResult := (operandA.asUInt < operandB.asUInt).asSInt
  }.elsewhen(isSLL) {
    aluResult := operandA << operandB(4, 0)
  }.elsewhen(isSRL) {
    aluResult := (operandA.asUInt >> operandB(4, 0)).asSInt
  }.elsewhen(isSRA) {
    aluResult := (operandA >> operandB(4, 0))// end of R type
  }.otherwise{
    aluResult := 0.S// NOP
    dontTouch(pc)
  }
   /* TODO: Add missing R-Type instructions here. Do not forget to implement a suitable default
   *       fetched instructions that are neither R-Type nor ADDI.
   */


  // -----------------------------------------
  // Memory
  // -----------------------------------------

  // No memory operations implemented in this basic CPU


  // -----------------------------------------
  // Write Back
  // -----------------------------------------

  val writeBackData = Wire(UInt(32.W))
  writeBackData := aluResult.asUInt
  when(rd=/=0.U) {
    regFile(rd) := writeBackData.asUInt
  }

  /*
   * TODO: Store "writeBackData" in register "rd" in regFile
   */

  // Check Result
  /*
   * TODO: Propagate "writeBackData" to the "check_res" output for testing purposes
   */
  io.check_res := writeBackData.asUInt

  // Update PC
  // no jumps or branches, next PC always reads next address from IMEM
  /*
   * TODO: Increment PC
   */
  pc := pc + 4.U
}
