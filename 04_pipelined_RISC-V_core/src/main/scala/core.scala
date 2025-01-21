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


// -----------------------------------------
// Register File
// -----------------------------------------

class regFileReadReq extends Bundle {
  val addr = Input(UInt(5.W))
}

class regFileReadResp extends Bundle {
  val data = Output(SInt(32.W))
}

class regFileWriteReq extends Bundle {
  val addr = Input(UInt(5.W))
  val data = Input(UInt(32.W))
  val en = Input(UInt(1.W))
}

class regFile extends Module {
  val io = IO(new Bundle {
    val readReq1 = new regFileReadReq
    val readResp1 = new regFileReadResp
    val readReq2 = new regFileReadReq
    val readResp2 = new regFileReadResp
    val writeReq = new regFileWriteReq
  })

  val regFile_ = Mem(32, UInt(32.W))
  regFile_(0.U) := 0.U

  io.readResp1.data := regFile_(io.readReq1.addr).asSInt
  io.readResp2.data := regFile_(io.readReq2.addr).asSInt

  when(io.writeReq.en.asBool && (io.writeReq.addr =/= 0.U)) {
    regFile_(io.writeReq.addr) := io.writeReq.data
  }
}


// -----------------------------------------
// Fetch Stage
// -----------------------------------------

class IF(BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val instr = Output(UInt(32.W))
  })

  val pc = RegInit(0.U(32.W))
  val IMem = Mem(4096, UInt(32.W))
  loadMemoryFromFile(IMem, BinaryFile)

  io.instr := IMem(pc >> 2.U)
  pc := pc + 4.U
}


// -----------------------------------------
// Decode Stage
// -----------------------------------------

class ID extends Module {
  val io = IO(new Bundle {
    val instr = Input(UInt(32.W))
    val uop = Output(uopc())
    val rs1 = Output(UInt(5.W))
    val rs2 = Output(UInt(5.W))
    val rd = Output(UInt(5.W))
    val imm = Output(SInt(12.W))
  })

  val opcode = io.instr(6, 0)
  val funct3 = io.instr(14, 12)
  val funct7 = io.instr(31, 25)
  val rs1 = io.instr(19, 15)
  val rs2 = io.instr(24, 20)
  val rd = io.instr(11, 7)
  val imm = io.instr(31, 20)
  val shifttype = io.instr(30)

  io.rs1 := rs1
  io.rs2 := rs2
  io.rd := rd
  io.imm := imm.asSInt

  io.uop := uopc.invalid
  switch(opcode) {
    is("b0110011".U) { // R-type
      switch(funct3) {
        is("b000".U) { // ADD, SUB
          when(funct7 === "b0000000".U) {
            io.uop := uopc.isADD
          } .otherwise {
            io.uop := uopc.isSUB
          }
        }
        is("b001".U) { io.uop := uopc.isSLL }
        is("b010".U) { io.uop := uopc.isSLT }
        is("b011".U) { io.uop := uopc.isSLTU }
        is("b100".U) { io.uop := uopc.isXOR }
        is("b101".U) { // SRL, SRA
          when(funct7 === "b0000000".U) {
            io.uop := uopc.isSRL
          } .otherwise {
            io.uop := uopc.isSRA
          }
        }
        is("b110".U) { io.uop := uopc.isOR }
        is("b111".U) { io.uop := uopc.isAND }
      }
    }
    is("b0010011".U) { // I-type
      switch(funct3) {
        is("b000".U) { io.uop := uopc.isADDI }
        is("b001".U) { io.uop := uopc.isSLLI }
        is("b010".U) { io.uop := uopc.isSLTI }
        is("b011".U) { io.uop := uopc.isSLTIU }
        is("b100".U) { io.uop := uopc.isXORI }
        is("b101".U) {
          when(shifttype === 0.U) {
            io.uop := uopc.isSRLI
          } .otherwise {
            io.uop := uopc.isSRAI
          }
        }
        is("b110".U) { io.uop := uopc.isORI }
        is("b111".U) { io.uop := uopc.isANDI }

      }
    }
  }
}


// -----------------------------------------
// Execute Stage
// -----------------------------------------

class EX extends Module {
  val io = IO(new Bundle {
    val uop = Input(uopc())
    val operandA = Input(SInt(32.W))
    val operandB = Input(SInt(32.W))
    val imm = Input(SInt(12.W))
    val aluResult = Output(UInt(32.W))
  })

  io.aluResult := "hFFFFFFFF".U
  switch(io.uop) {
    is(uopc.isADD) { io.aluResult := (io.operandA + io.operandB).asUInt }
    is(uopc.isSUB) { io.aluResult := (io.operandA - io.operandB).asUInt }
    is(uopc.isXOR) { io.aluResult := (io.operandA ^ io.operandB).asUInt }
    is(uopc.isOR) { io.aluResult := (io.operandA | io.operandB).asUInt }
    is(uopc.isAND) { io.aluResult := (io.operandA & io.operandB).asUInt }
    is(uopc.isSLL) { io.aluResult := (io.operandA << io.operandB(4, 0)).asUInt }
    is(uopc.isSRL) { io.aluResult := (io.operandA >> io.operandB(4, 0)).asUInt }
    is(uopc.isSRA) { io.aluResult := (io.operandA.asSInt >> io.operandB(4, 0)).asUInt }
    is(uopc.isSLT) { io.aluResult := (io.operandA.asSInt < io.operandB.asSInt).asUInt }
    is(uopc.isSLTU) { io.aluResult := (io.operandA.asUInt < io.operandB.asUInt).asUInt }

    is(uopc.isADDI) { io.aluResult := (io.operandA + io.imm).asUInt }
    is(uopc.isSLTI) { io.aluResult := (io.operandA.asSInt < io.imm).asUInt }
    is(uopc.isSLTIU) { io.aluResult := (io.operandA.asUInt < io.imm.asUInt).asUInt }
    is(uopc.isXORI) { io.aluResult := (io.operandA ^ io.imm).asUInt }
    is(uopc.isORI) { io.aluResult := (io.operandA | io.imm).asUInt }
    is(uopc.isANDI) { io.aluResult := (io.operandA & io.imm).asUInt }
    is(uopc.isSLLI) { io.aluResult := (io.operandA << io.imm(4, 0)).asUInt }
    is(uopc.isSRLI) { io.aluResult := (io.operandA >> io.imm(4, 0)).asUInt }
    is(uopc.isSRAI) { io.aluResult := (io.operandA.asSInt >> io.imm(4, 0)).asUInt }
  }
}


// -----------------------------------------
// Memory Stage
// -----------------------------------------

class MEM extends Module {
  val io = IO(new Bundle {
    val aluResult = Input(UInt(32.W))
    val memResult = Output(UInt(32.W))
  })

  // No memory operations implemented in this basic CPU
  io.memResult := io.aluResult
}


// -----------------------------------------
// Writeback Stage
// -----------------------------------------

class WB extends Module {
  val io = IO(new Bundle {
    val memResult = Input(UInt(32.W)) // Input result
    val rd = Input(UInt(5.W))
    val writeBackData = Output(UInt(32.W)) // Output result
    val writeAddress = Output(UInt(5.W))
    val writeEnable = Output(UInt(1.W))
  })

  io.writeBackData := io.memResult
  io.writeAddress := io.rd
  io.writeEnable := 1.U
}


// -----------------------------------------
// IF-Barrier
// -----------------------------------------

class IFBarrier extends Module {
  val io = IO(new Bundle {
    val instr = Input(UInt(32.W))
    val instr_out = Output(UInt(32.W))
  })

  val instrReg = RegNext(io.instr)

  io.instr_out := instrReg
}


// -----------------------------------------
// ID-Barrier
// -----------------------------------------

class IDBarrier extends Module {
  val io = IO(new Bundle {
    val uop = Input(uopc())
    val rs1 = Input(SInt(32.W))
    val rs2 = Input(SInt(32.W))
    val rd = Input(UInt(5.W))
    val imm = Input(SInt(12.W))

    val uop_out = Output(uopc())
    val rs1_out = Output(SInt(32.W))
    val rs2_out = Output(SInt(32.W))
    val rd_out = Output(UInt(5.W))
    val imm_out = Output(SInt(12.W))
  })

  val uopReg = RegNext(io.uop)
  val rs1Reg = RegNext(io.rs1)
  val rs2Reg = RegNext(io.rs2)
  val rdReg = RegNext(io.rd)
  val immReg = RegNext(io.imm)
  // val uopReg = RegInit(io.uop)
  // val rs1Reg = RegInit(0.U(32.W))
  // val rs2Reg = RegInit(0.U(32.W))
  // val rdReg = RegInit(0.U(5.W))
  // val immReg = RegInit(0.U(12.W))

  // uopReg := io.uop
  // rs1Reg := io.rs1
  // rs2Reg := io.rs2
  // rdReg := io.rd
  // immReg := io.imm

  io.uop_out := uopReg
  io.rs1_out := rs1Reg
  io.rs2_out := rs2Reg
  io.rd_out := rdReg
  io.imm_out := immReg
}


// -----------------------------------------
// EX-Barrier
// -----------------------------------------

class EXBarrier extends Module {
  val io = IO(new Bundle {
    val aluResult = Input(UInt(32.W))
    val rd = Input(UInt(5.W))
    val aluResult_out = Output(UInt(32.W))
    val rd_out = Output(UInt(5.W))
  })

  val aluResultReg = RegNext(io.aluResult)
  val rdReg = RegNext(io.rd)

  io.aluResult_out := aluResultReg
  io.rd_out := rdReg
}


// -----------------------------------------
// MEM-Barrier
// -----------------------------------------

class MEMBarrier extends Module {
  val io = IO(new Bundle {
    val memResult = Input(UInt(32.W))
    val rd = Input(UInt(5.W))
    val memResult_out = Output(UInt(32.W))
    val rd_out = Output(UInt(5.W))
  })

  val memResultReg = RegNext(io.memResult)
  val rdReg = RegNext(io.rd)

  io.memResult_out := memResultReg
  io.rd_out := rdReg
}


// -----------------------------------------
// WB-Barrier
// -----------------------------------------

class WBBarrier extends Module {
  val io = IO(new Bundle {
    val writeBackData = Input(UInt(32.W))
    val writeBackData_out = Output(UInt(32.W))
  })

  val writeBackDataReg = RegNext(io.writeBackData)

  io.writeBackData_out := writeBackDataReg
}


class PipelinedRV32Icore(BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res = Output(UInt(32.W))
  })

  // Instantiate Barriers
  val ifBarrier = Module(new IFBarrier)
  val idBarrier = Module(new IDBarrier)
  val exBarrier = Module(new EXBarrier)
  val memBarrier = Module(new MEMBarrier)
  val wbBarrier = Module(new WBBarrier)

  // Instantiate Pipeline Stages
  val ifStage = Module(new IF(BinaryFile))
  val idStage = Module(new ID)
  val exStage = Module(new EX)
  val memStage = Module(new MEM)
  val wbStage = Module(new WB)

  val regFile = Module(new regFile)

  // IF Stage connections
  ifBarrier.io.instr := ifStage.io.instr

  // ID Stage connections
  idStage.io.instr := ifBarrier.io.instr_out
  regFile.io.readReq1.addr := idStage.io.rs1
  regFile.io.readReq2.addr := idStage.io.rs2

  // ID Barrier connections
  idBarrier.io.uop := idStage.io.uop
  idBarrier.io.rs1 := regFile.io.readResp1.data
  idBarrier.io.rs2 := regFile.io.readResp2.data
  idBarrier.io.rd := idStage.io.rd
  idBarrier.io.imm := idStage.io.imm

  // EX Stage connections
  exStage.io.uop := idBarrier.io.uop_out
  exStage.io.operandA := idBarrier.io.rs1_out
  exStage.io.operandB := idBarrier.io.rs2_out
  exStage.io.imm := idBarrier.io.imm_out

  // EX Barrier connections
  exBarrier.io.aluResult := exStage.io.aluResult
  exBarrier.io.rd := idBarrier.io.rd_out

  // MEM Stage connections
  memStage.io.aluResult := exBarrier.io.aluResult_out

  // MEM Barrier connections
  memBarrier.io.memResult := memStage.io.memResult
  memBarrier.io.rd := exBarrier.io.rd_out

  // WB Stage connections
  wbStage.io.memResult := memBarrier.io.memResult_out
  wbStage.io.rd := memBarrier.io.rd_out

  // WB Barrier connection
  wbBarrier.io.writeBackData := wbStage.io.writeBackData

  // Register File write connections
  regFile.io.writeReq.addr := wbStage.io.writeAddress
  regFile.io.writeReq.data := wbStage.io.writeBackData
  regFile.io.writeReq.en := wbStage.io.writeEnable

  // Output connection
  io.check_res := wbBarrier.io.writeBackData_out

  printf(p"writeBackData: 0x${Hexadecimal(wbBarrier.io.writeBackData_out)}\n")
}