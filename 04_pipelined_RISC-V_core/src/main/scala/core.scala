// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/15/2023 by Tobias Jauch (@tojauch)

/*
The goal of this task is to extend the 5-stage multi-cycle 32-bit RISC-V core from the previous task to a pipelined processor. 
All steps and stages have the same functionality as in the multi-cycle version from task 03, but are supposed to handle different instructions in each stage simultaneously.
This design implements a pipelined RISC-V 32-bit core with five stages: IF (Fetch), ID (Decode), EX (Execute), MEM (Memory), and WB (Writeback).

    Data Types:
        The uopc enumeration data type (enum) defines micro-operation codes representing ALU operations according to the RV32I subset used in the previous tasks.

    Register File (regFile):
        The regFile module represents the register file, which has read and write ports.
        It consists of a 32-entry register file (x0 is hard-wired to zero).
        Reading from and writing to the register file is controlled by the read request (regFileReadReq), read response (regFileReadResp), and write request (regFileWriteReq) interfaces.

    Fetch Stage (IF Module):
        The IF module represents the instruction fetch stage.
        It includes an instruction memory (IMem) of size 4096 words (32-bit each).
        Instructions are loaded from a binary file (provided to the testbench as a parameter) during initialization.
        The program counter (PC) is used as an address to access the instruction memory, and one instruction is fetched in each cycle.

    Decode Stage (ID Module):
        The ID module performs instruction decoding and generates control signals.
        It extracts opcode, operands, and immediate values from the instruction.
        It uses the uopc (micro-operation code) Enum to determine the micro-operation (uop) and sets control signals accordingly.
        The register file requests are generated based on the operands in the instruction.

    Execute Stage (EX Module):
        The EX module performs the arithmetic or logic operation based on the micro-operation code.
        It takes two operands and produces the result (aluResult).

    Memory Stage (MEM Module):
        The MEM module does not perform any memory operations in this basic CPU design.

    Writeback Stage (WB Module):
        The WB module writes the result back to the register file.

    IF, ID, EX, MEM, WB Barriers:
        IFBarrier, IDBarrier, EXBarrier, MEMBarrier, and WBBarrier modules serve as pipeline registers to separate the pipeline stages.
        They hold the intermediate results of each stage until the next clock cycle.

    PipelinedRV32Icore (PipelinedRV32Icore Module):
        The top-level module that connects all the pipeline stages, barriers and the register file.
        It interfaces with the external world through check_res, which is the result produced by the core.

Overall Execution Flow:

    1) Instructions are fetched from the instruction memory in the IF stage.
    2) The fetched instruction is decoded in the ID stage, and the corresponding micro-operation code is determined.
    3) The EX stage executes the operation using the operands.
    4) The MEM stage does not perform any memory operations in this design.
    5) The result is written back to the register file in the WB stage.

Note that this design only represents a simplified RISC-V pipeline. The structure could be equipped with further instructions and extension to support a real RISC-V ISA.
*/

package core_tile

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile


// -----------------------------------------
// Global Definitions and Data Types
// -----------------------------------------

object uopc extends ChiselEnum {
//// ADD for other ISAs
  val isADD   = Value(0x01.U)
  val isSUB   = Value(0x02.U)
  val isXOR   = Value(0x03.U)
  val isOR    = Value(0x04.U)
  val isAND   = Value(0x05.U)
  val isSLL   = Value(0x06.U)
  val isSRL   = Value(0x07.U)
  val isSRA   = Value(0x08.U)
  val isSLT   = Value(0x09.U)
  val isSLTU  = Value(0x0A.U)

  val isADDI  = Value(0x10.U)
  val isSLTI  = Value(0x11.U)
  val isSLTIU = Value(0x12.U)
  val isANDI  = Value(0x13.U)
  val isORI   = Value(0x14.U)
  val isXORI  = Value(0x15.U)
  val isSLLI  = Value(0x16.U)
  val isSRLI  = Value(0x17.U)
  val isSRAI  = Value(0x18.U)

  val invalid = Value(0xFF.U)
}

import uopc._
val fetch :: decode :: execute :: memory :: writeback :: Nil = Enum(5) // Enum datatype to define the stages of the processor FSM
val stage = RegInit(fetch)

// -----------------------------------------
// Register File
// -----------------------------------------

class regFileReadReq extends Bundle {
  // what signals does a read request need?
  val addr = UInt(5.W)  // First register rs1 address (5 bits for a 32-register file)
  // val addr2 = UInt(5.W)  // Second register rs2 address (5 bits for a 32-register file)
}

class regFileReadResp extends Bundle {
  // what signals does a read response need?
  val data = UInt(32.W)  // Data that is read from the first register
  //val data2 = UInt(32.W)  // Data that is read from the second register
}

class regFileWriteReq extends Bundle {
  // what signals does a write request need?
  val addr = UInt(5.W)    // Register address (5 bits for the 32-register file)
  val data = UInt(32.W)   // Data to be written to the register
  val writeEnable = Bool() // Signal to enable or disable the write operation
}

class regFile extends Module {
  val io = IO(new Bundle {
    val req1  = new regFileReadReq
    val req2  = new regFileReadReq

    val resp1 = new regFileReadResp
    val resp2 = new regFileReadResp

    val writereq = new regFileWriteReq
    // how many read and write ports do you need to handle all requests
    // from the ipeline to the register file simultaneously?

  })
  /*
  TODO: Initialize the register file as described in the task
        and handle the read and write requests
 */


  val regFile = Mem(32, UInt(32.W))
  regFile(0) := 0.U

  io.resp1.data := regFile(io.req1.addr)
  io.resp2.data := regFile(io.req2.addr)
  when(io.writereq.addr =/= 0.U && io.writereq.writeEnable === 1){
    regFile(io.writereq.addr) := io.writereq.data
  }




}
// -----------------------------------------
// Fetch Stage
// -----------------------------------------

class IF (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    //Inputs
    val old_pc = Input(UInt(16.W)) // fedback from IF barrier

    // Outputs
    val instr = Output(UInt(32.W))
    val pc = Output(UInt(16.W))
  })
  val IMem = Mem(4096, UInt(32.W))
  loadMemoryFromFile(IMem, BinaryFile)
  //val ifBarrier = Module(new IFBarrier)//  This is to be done in the main function
  io.instr := IMem(old_pc >> 2.U)
  //ifBarrier.io.instr := io.instr// This is to be done in the main function
  io.pc := pc_old + 4.U
  // io.pc := pc_reg

  //IF-> IF Barrier(registers)
}

// -----------------------------------------
// Decode Stage
// -----------------------------------------

class ID extends Module {
  val io = IO(new Bundle {
    //IF-> IF Barrier->ID

    // Inputs
    val instr_decode = Input(UInt(32.W)) // from IF_Barrier

    // Outputs
    val immediate_opB  = Output(SInt(32.W)) // immediate value for the I type
    val operandA = Output(SInt(32.W)) //regfile operation in the main function
    val operandB = Output(SInt(32.W)) //regfile operation in the main function
    val rd_out = Output(UInt(5.W)) //address of RD for result writing in WB
    val shifttype_out = Output(UInt(1.W)) // Shift type needed for operation in EX
    val upoc = Output(upoc())  // The operation Micro coded

  })
  // Internal Signals
  val rs1 = Wire(UInt(5.W))
  val rs2 = Wire(UInt(5.W))
  val rd =  Wire(UInt(5.W))
  val imm = Wire(UInt(12.W))
  val shifttype = Wire(UInt(1.W))
  val funct3 = Wire(UInt(3.W))
  val funct7 = Wire(UInt(7.W))
  val opcode = Wire(UInt(7.W))

  //Value assignment fot internal registers

  opcode = instr_decode(6, 0)
  funct3 = instr_decode(14, 12)
  funct7 = instr_decode(31, 25)
  shifttype = instr_decode(30)
  rd = instr_decode(11, 7)
  rs1 = instr_decode(19, 15)
  rs2 = instr_decode(24, 20)
  imm =  instr_decode(31, 20)
    when(opcode === "b0110011".U && funct3 === "b000".U && funct7 === "b0000000".U) {
      io.upoc := isADD
    }.elsewhen(opcode === "b0110011".U && funct3 === "b000".U && funct7 === "b0100000".U) {
     io.upoc := isSUB
    }.elsewhen(opcode === "b0110011".U && funct3 === "b111".U && funct7 === "b0000000".U) {
     io.upoc := isAND
    }.elsewhen(opcode === "b0110011".U && funct3 === "b110".U && funct7 === "b0000000".U) {
     io.upoc := isOR
    }.elsewhen(opcode === "b0110011".U && funct3 === "b100".U && funct7 === "b0000000".U) {
     io.upoc := isXOR
    }.elsewhen(opcode === "b0110011".U && funct3 === "b011".U && funct7 === "b0000000".U) {
     io.upoc := isSLTU
    }.elsewhen(opcode === "b0110011".U && funct3 === "b010".U && funct7 === "b0000000".U) {
     io.upoc := isSLT
    }.elsewhen(opcode === "b0110011".U && funct3 === "b001".U && funct7 === "b0000000".U) {
     io.upoc := isSLL
    }.elsewhen(opcode === "b0110011".U && funct3 === "b101".U && funct7 === "b0000000".U) {
     io.upoc := isSRL
    }.elsewhen(opcode === "b0110011".U && funct3 === "b101".U && funct7 === "b0100000".U) {
     io.upoc := isSRA
    }.elsewhen(opcode === "b0010011".U && funct3 === "b000".U) {
     io.upoc := isADDI
    }.elsewhen(opcode === "b0010011".U && funct3 === "b010".U) {
     io.upoc := isSLTI
    }.elsewhen(opcode === "b0010011".U && funct3 === "b011".U) {
     io.upoc := isSLTIU
    }.elsewhen(opcode === "b0010011".U && funct3 === "b111".U) {
     io.upoc := isANDI
    }.elsewhen(opcode === "b0010011".U && funct3 === "b110".U) {
     io.upoc := isORI
    }.elsewhen(opcode === "b0010011".U && funct3 === "b100".U) {
     io.upoc := isXORI
    }.elsewhen(opcode === "b0010011".U && funct3 === "b001".U) {
     io.upoc := isSLLI
    }.elsewhen(opcode === "b0010011".U && funct3 === "b101".U && shifttype === "b0".U) {
     io.upoc := isSRLI
    }.elsewhen(opcode === "b0010011".U && funct3 === "b101".U && shifttype === "b1".U) {
     io.upoc := isSRAI
    }.otherwise{
     io.upoc := invalid
    }
  // Operand A nd B are referenced from reguster file in main function
    io.immediate_opB := imm.asSInt
    io.shifttype_out := shifttype.asUInt
    io.rd_out := rd
  //IF-> IF Barrier->ID-> ID Barrier(registers)


   /*val Register1 := Module(new regFile)  NEEDS TO BE GIVEN OUTSIDE
   io.Register1.req1.addr:= io.rs1
   io.operandA  := io.Register1.resp1.data
  when( opcode === "b0010011".U){
    operandB := imm.asSInt
  }.elsewhen(opcode === "b0110011".U){
    io.Register1.req2.addr:= io.rs2
    io.operandB := io.Register1.resp2.data
  }
    io.rd_out := io.rd

   /* val idBarrier := Module(new IDBarrier) NEEDS TO BE GIVEN OUTSIDE
    idBarrier.io.upoc := io.upoc
    idBarrier.io.operandA := io.operandA
    idBarrier.io.operandB := io.operandB
    idBarrier.io.rd_out := io.rd_out

    stage := execute*/ */

}

// -----------------------------------------
// Execute Stage
// -----------------------------------------

class EX extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
  /// Inputs
    val operandA  = Input(SInt(32.W))
    val operandB  = Input(SInt(32.W))
    val Immediate_opB_in  = Input(SInt(32.W))
    val uopc = Input(upoc())
    val rd_in = Input(UInt(5.W))

    // Outputs
    val aluresult  = Output(UInt(32.W)
    val rd_out = Output(UInt(5.W))


  })

  /* 
  val idBarrier1 := Module(new IDBarrier)     //// In the main function

  io.operandA := idBarrier1.io.opA
  io.operandB := idBarrier1.io.opB
  io.upoc := idBarrier1.io.operation
  io.rd := idBarrier1.io.rd_Barrier_out

   */

      /*
     * TODO: Implement execute stage
     */

    when(uopc === isADDI) { // start of I type
      io.aluResult := (operandA + Immediate_opB_in).asUInt
    }.elsewhen(uopc ===isSLTI) {
      io.aluResult := (operandA  < Immediate_opB_in).asUInt
    }.elsewhen(uopc ===isSLTIU) {
      io.aluResult := (operandA.asUInt < (Immediate_opB_in).asUInt).asUInt
    }.elsewhen(uopc === isANDI) {
      io.aluResult := (operandA & Immediate_opB_in).asUInt
    }.elsewhen(uopc === isORI) {
      io.aluResult := (operandA | Immediate_opB_in).asUInt
    }.elsewhen(uopc === isXORI) {
      io.aluResult := (operandA ^ Immediate_opB_in).asUInt
    }.elsewhen(uopc === isSLLI) {
      io.aluResult := (operandA.asUInt << rs2).asUInt
    }.elsewhen(uopc === isSRLI) {
      io.aluResult := (operandA.asUInt >> rs2).asUInt
    }.elsewhen(uopc === isSRAI) {
      io.aluResult := (operandA.asSInt >> rs2).asUInt//end of I Type
    }.elsewhen(uopc === isADD) { // start of R type
      io.aluResult := (operandA + operandB).asUInt
    }.elsewhen(uopc === isSUB) {
      io.aluResult := (operandA - operandB).asUInt
    }.elsewhen(uopc === isAND) {
      io.aluResult := (operandA & operandB).asUInt
    }.elsewhen(uopc === isOR) {
      io.aluResult := (operandA | operandB).asUInt
    }.elsewhen(uopc === isXOR) {
      io.aluResult := (operandA ^ operandB).asUInt
    }.elsewhen(uopc === isSLT) {
      io.aluResult := (operandA < operandB).asUInt
    }.elsewhen(uopc === isSLTU) {
      io.aluResult := (operandA.asUInt < operandB.asUInt).asUInt
    }.elsewhen(uopc === isSLL) {
      io.aluResult := (operandA << operandB(4, 0).asUInt).asUInt
    }.elsewhen(uopc === isSRL) {
      io.aluResult := (operandA.asUInt >> operandB(4, 0).asUInt).asUInt
    }.elsewhen(iuopc === sSRA) {
      io.aluResult := (operandA >> operandB(4, 0).asUInt).asUInt// end of R type
    }.otherwise{
      io.aluResult := "hFFFFFFFF".U// NOP
      //dontTouch(pc)
    }
    io.rd_out := io.rd_in
   /* val exBarrier = Module(new EXBarrier)         Outside memory
    exBarrier.io.aluResult := io.aluResult
    exBarrier.io.rd := io.rd_out*/
  }

}

// -----------------------------------------
// Memory Stage
// -----------------------------------------

class MEM extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    // Inputs
    val aluResult_in = Input(UInt(32.W))
    val rd_in = Input(UInt(5.W))
    // Outputs
    val Writeback_out = Output(UInt(32.W))
    val rd_out = Output(UInt(5.W))

  }

  // No memory operations implemented in this basic CPU   // done in Main function
    /*val exBarrier = Module(new EXBarrier)
    val memBarrier = Module(new MEMBarrier)
    io.aluResult := io.exBarrier.Writeback

    io.rd := io.exBarrier.rd_barrier_out
    io.rd_out := io.rd
    io.memBarrier.Writeback := io.Writeback
    io.memBarrier.rd := io.rd_out
    state := WriteBack

     */
    io.Writeback_out := io.aluResult_in
    io.rd_out := io.rd_in

}


// -----------------------------------------
// Writeback Stage
// -----------------------------------------

class WB extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this pipeline stage need?
    val Writeback_data = Input(UInt(32.W))
    val Writeback_address = Input(UInt(5.W)) // rd value
    val checkres = Output(UInt(32.W))

  })
  // Internal signals
  val write_en = Wire(UInt(1.W))
  write_en := 1.U

  // Write in registter file is carried in main function

  /*val memBarrier = Module(new MEMBarrier)
  val wbBarrier = Module(new WBBarrier)
  io.Writeback_data = io.memBarrier.Writeback_Barrierout
  io.Writeback_address = io.memBarrier.Writeback.rd_barrier_out
  val Register1 := Module(new regFile)
  io.Register1.writereq.data :=  io.Writeback_data
  io.Register1.writereq.writeEnable := io.write_en
  io.Register1.writereq.addr := io.Writeback_address
   */

  // Output
  io.checkres := io.Writeback_data
}



// -----------------------------------------
// IF-Barrier
// -----------------------------------------

class IFBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    val pc_in = Input(UInt(32.W))
    val instr_in = Input(UInt(32.W))
    val instr_out = Output(UInt(32.W))
    val pc_out = Output(UInt(32.W))

  })
  // Registers
  val instr_reg = RegInit(0.U(32.W)
  instr_reg := io.instr_in
  val pc_reg := RegInit(0.U(32.W)
  pc_reg := io.pc_in

  //Outputs
  io.instr_out := instr_reg
  io.pc_out := pc_reg
 }


// -----------------------------------------
// ID-Barrier
// -----------------------------------------

class IDBarrier extends Module {
  val io = IO(new Bundle {

    // What inputs and / or outputs does this barrier need?
    val operandA_in = Input(SInt(32.W))
    val operandB_in = Input(SInt(32.W))
    val immediate_in = Input(SInt(32.W))
    val upoc_in = Input(upoc())
    val rd_in = Input(UInt(5.W))
    // Outputs
    val opA_out = Output(SInt(32.W))
    val opB_out = Output(SInt(32.W))
    val operation_out = Output(upoc())
    val rd_out = Output(UInt(5.W))
    val immediate_out = Output(SInt(32.W))
  })
// Internal Registers
  val operandA_reg = RegInit(0.asSInt(32.W))
  val operandB_reg = RegInit(0.asSInt(32.W))
  val Immediate_reg = RegInit(0.asSInt(32.W))
  val upo_reg = RegInit(uopc.isADD) // the data type is an enum!
  val rd_reg = RegInit(0.asUInt(5.W))
  operandA_reg := io.operandA_in
  operandB_reg := io.operandB_in
  rd_reg := io.rd_in
  upo_reg := io.upoc_in
  io.opA_out := operandA_reg
  io.opB_out := operandB_reg
  io.operation_out := upo_reg
  io.rd_out := rd_reg
  io.immediate_out := Immediate_reg
}


// -----------------------------------------
// EX-Barrier
// -----------------------------------------

class EXBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    // Inputs
    val aluResult_in = Input(UInt(32.W))
    val rd_in = Input(UInt(5.W))
    // outputs
    val Writeback = Output(UInt(32.W))
    val rd_barrier_out = Output (UInt(5.W))
  })
  /// Internal registers
  val rd_reg = RegInit(0.asUInt(5.W))
  val aluResult_reg = RegInit(0.asUInt(32.W))
  aluResult_reg := io.aluResult_in
  rd_reg := io.rd_in
  // Ouputs
  io.Writeback := aluResult_reg
  io.rd_barrier_out := rd_reg
}


// -----------------------------------------
// MEM-Barrier
// -----------------------------------------

class MEMBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    // Inputs
    val Writeback_in = Input(UInt(32.W))
    val rd_in = Input(UInt(5.W))
    // Outputs
    val Writeback_Barrierout = Output(UInt(32.W))
    val rd_barrier_out = Output (UInt(5.W))

  })
  // Internal registers
  val Writeback_reg := RegInit(0.asUInt(32.W)))
  val rd_reg = RegInit(0.asUInt(5.W))
  Writeback_reg := io.Writeback_in
  rd_reg := io.rd_in

  // Outputs
  io.Writeback_Barrierout := Writeback_reg
  io.rd_barrier_out := rd_reg
}


// -----------------------------------------
// WB-Barrier
// -----------------------------------------

class WBBarrier extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does this barrier need?
    val check_res_in = Input(UInt(32.W))
    val check_res_out = Output(UInt(32.W))

  })
  // Internal registers
  val check_res_reg = RegInit(0.asUInt(32.W))
  check_res_reg := io.check_res
  // Outputs
  io.check_res_out := check_res_reg

}



class PipelinedRV32Icore (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res_final = Output(UInt(32.W))
  })


  /* 
   * TODO: Instantiate Barriers
   */
  val if_barrier = Module(new IFBarrier)
  val id_barrier = Module(new IDBarrier)
  val ex_barrier = Module(new EXBarrier)
  val mem_barrier = Module(new MEMBarrier)
  val wb_barrier = Module(new WBBarrier)


  /* 
   * TODO: Instantiate Pipeline Stages
   */
  val if_stage = Module(new IF(BinaryFile))
  val id_stage = Module(new ID)
  val ex_stage = Module(new EX)
  val mem_stage = Module(new MEM)
  val wb_stage = Module(new WB)


  /* 
   * TODO: Instantiate Register File
   */
  val regFile = Module(new regFile)

  // feedback for pc in IF
  // IF -> IF barrier
  //IF barrier -> ID
  // Register file memory read:- op_A and op_B (in-address and out-resp)
  //ID-> ID barrier
  // ID barrier-> EX
  // EX-> EX barrier
  //EX barrier-> MEM
  //MEM-> MEMbarrier
  //Membarrier-> WB
  // Register file memory write:- Write_data , Write address, Write_en
  //WB-> WBbarrier
  // Final out assignment from WB barrier



  // all wires between the modules are connected here

  io.check_res_final := 0.U // necessary to make the empty design buildable TODO: change this

}

