// ADS I Class Project
// Pipelined RISC-V Core with Hazard Detetcion and Resolution
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 05/21/2024 by Andro Mazmishvili (@Andrew8846)

/*
The goal of this task is to equip the pipelined 5-stage 32-bit RISC-V core from the previous task with a forwarding unit that takes care of hazard detetction and hazard resolution.
The functionality is the same as in task 4, but the core should now also be able to also process instructions with operands depending on the outcome of a previous instruction without stalling.

In addition to the pipelined design from task 4, you need to implement the following modules and functionality:

    Hazard Detection and Forwarding:
        Forwarding Unit: Determines if and from where data should be forwarded to resolve hazards. 
                         Resolves data hazards by forwarding the correct values from later pipeline stages to earlier ones.
                         - Inputs: Register identifiers from the ID, EX, MEM, and WB stages.
                         - Outputs: Forwarding select signals (forwardA and forwardB) indicating where to forward the values from.

        The forwarding logic utilizes multiplexers to select the correct operand values based on forwarding decisions.

Make sure that data hazards (dependencies between instructions in the pipeline) are detected and resolved without stalling the pipeline. For additional information, you can revise the ADS I lecture slides (6-25ff).

Note this design only represents a simplified RISC-V pipeline. The structure could be equipped with further instructions and extension to support a real RISC-V ISA.
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
  val isBNE   = Value(0x11.U)
  val isBLT   = Value(0x12.U)
  val isBLTU   = Value(0x13.U)
  val isBGE   = Value(0x14.U)
  val isBGEU   = Value(0x15.U)

  val invalid = Value(0xFF.U)
}

import uopc._


// -----------------------------------------
// Register File
// -----------------------------------------

class regFileReadReq extends Bundle {
    val addr  = Input(UInt(5.W))
}

class regFileReadResp extends Bundle {
    val data  = Output(UInt(32.W))
}

class regFileWriteReq extends Bundle {
    val addr  = Input(UInt(5.W))
    val data  = Input(UInt(32.W))
    val wr_en = Input(Bool())
}

class regFile extends Module {
  val io = IO(new Bundle {
    val req_1  = new regFileReadReq
    val resp_1 = new regFileReadResp
    val req_2  = new regFileReadReq
    val resp_2 = new regFileReadResp
    val req_3  = new regFileWriteReq
})

  val regFile = Mem(32, UInt(32.W))
  regFile(0) := 0.U                           // hard-wired zero for x0
  
  def readWithForwarding(readAddr: UInt): UInt = {
    val isZeroReg = readAddr === 0.U
    val isWriting = io.req_3.wr_en && (io.req_3.addr === readAddr)
    
    Mux(isZeroReg, 0.U,                    // Return 0 if reading x0
        Mux(isWriting, io.req_3.data,      // Forward written data if same address
            regFile(readAddr)))             // Otherwise read from register file
  }

  // Connect read ports with forwarding logic
  io.resp_1.data := readWithForwarding(io.req_1.addr)
  io.resp_2.data := readWithForwarding(io.req_2.addr)

  // Write logic
  when(io.req_3.wr_en && (io.req_3.addr =/= 0.U)) {
    regFile(io.req_3.addr) := io.req_3.data
  }
}

class ForwardingUnit extends Module {
  val io = IO(new Bundle {
    // What inputs and / or outputs does the forwarding unit need?
    val rs1_addr = Input(UInt(5.W))
    val rs2_addr = Input(UInt(5.W))
    val rd_addr_exbarrier  = Input(UInt(5.W))
    val rd_addr_membarrier  = Input(UInt(5.W))
    val operandA_frwd = Output(UInt(2.W))
    val operandB_frwd = Output(UInt(2.W))
  })

  /* TODO:
   Forwarding Selection:
   Select the appropriate value to forward from one stage to another based on the hazard checks.
*/
  // logic for forwarding
  when((io.rs1_addr === io.rd_addr_membarrier) && io.rs1_addr =/= 0.U)
  {
    io.operandA_frwd := 2.U
  }.elsewhen((io.rs1_addr === io.rd_addr_exbarrier)  && (io.rs1_addr =/= 0.U)){
      io.operandA_frwd := 1.U
    }.otherwise{
     io.operandA_frwd := 0.U
  }
  when((io.rs2_addr === io.rd_addr_membarrier) && (io.rs2_addr =/= 0.U)) {
    io.operandB_frwd := 2.U
  }.elsewhen((io.rs2_addr === io.rd_addr_exbarrier) && (io.rs2_addr =/= 0.U)){
      io.operandB_frwd := 1.U
    }.otherwise{
    io.operandB_frwd := 0.U
  }
}


// -----------------------------------------
// Fetch Stage
// -----------------------------------------

class IF (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val JumpPC = Input(UInt(32.W)) // feedback from decode stage
    val Jumpflush = Input(UInt(1.W)) //feedback from decode stage
    val BranchPC = Input(UInt(32.W))
    val Branchtaken = Input(UInt(1.W))
    val instr = Output(UInt(32.W))
    val Computed_PC = Output(UInt(32.W))
  })
  // bool logic for conditional and unconditional jumps
  val nextPC = Wire(UInt(32.W))

  val IMem = Mem(4096, UInt(32.W))
  loadMemoryFromFile(IMem, BinaryFile)

  val PC = RegInit(0.U(32.W))
  io.instr := IMem(PC >> 2.U)

  // Update PC
  // no jumps or branches, next PC always reads next address from IMEM
  when(io.Jumpflush === 1.U) {
    nextPC := io.JumpPC
  }.elsewhen(io.Branchtaken === 1.U){
    nextPC := io.BranchPC
    }.otherwise {
    nextPC := PC + 4.U
  }
  io.Computed_PC := PC
  PC := nextPC
}

// -----------------------------------------
// Decode Stage  can be used to couont reyurn PC here
// -----------------------------------------

class ID extends Module {
  val io = IO(new Bundle {
    val regFileReq_A = Flipped(new regFileReadReq)
    val regFileResp_A = Flipped(new regFileReadResp)
    val regFileReq_B = Flipped(new regFileReadReq)
    val regFileResp_B = Flipped(new regFileReadResp)
    val instr = Input(UInt(32.W))
    val PC = Input(UInt(32.W))
    val uop = Output(uopc())
    val rd = Output(UInt(5.W))
    val rs1 = Output(UInt(5.W))
    val rs2 = Output(UInt(5.W))
    val operandA = Output(UInt(32.W))
    val operandB = Output(UInt(32.W))
    val Jump_flag = Output(UInt(1.W)) // Feedback to the IF/ID barrier
    val Jumpaddress = Output(UInt(32.W)) // Feedback to IF stage
    val Branchaddress = Output(UInt(32.W))
  })

  val opcode = io.instr(6, 0)
  val funct3 = io.instr(14, 12)
  val rs1 = io.instr(19, 15)
  val funct7 = io.instr(31, 25) // R-Type
  val rs2 = io.instr(24, 20)
  val imm_12   = io.instr(31)           // imm[12]
  val imm_11   = io.instr(7)            // imm[11]
  val imm_10_5 = io.instr(30, 25)       // imm[10:5]
  val imm_4_1  = io.instr(11, 8)        // imm[4:1]
  val offset_branch_raw = Cat(imm_12, imm_11, imm_10_5, imm_4_1, 0.U(1.W)) //offset value for the branch


  // Immediate handling
  val imm = io.instr(31, 20).asSInt // I-Type immediate
  val offset_jump = io.instr(31, 12).asSInt // Jump immediate (sign-extended)

  val signextended_imm = Wire(SInt(32.W))
  val wordaligned_imm = Wire(SInt(32.W))
  val jump_internal = Wire(UInt(32.W))
  val branch_offset = Wire(SInt(32.W))
  val branch_address = Wire(UInt(32.W))

  // Compute jump target
  signextended_imm := offset_jump
  wordaligned_imm := signextended_imm << 2 // Multiply by 4 for word-aligned offset
  jump_internal := (io.PC.asSInt + wordaligned_imm).asUInt
  // Branch target address intialised
  branch_offset := offset_branch_raw.asSInt
  branch_address :=  (io.PC.asSInt + branch_offset.asSInt).asUInt

  // Default values for outputs
  io.Jump_flag := 0.U
  io.Jumpaddress := 0.U
  io.uop := invalid
  io.Branchaddress := 0.U

  // Operation Selection (ROM Table)
  when(opcode === "b0110011".U) { // R-Type
    when(funct3 === "b000".U) {
      when(funct7 === "b0000000".U) {
        io.uop := isADD
      }.elsewhen(funct7 === "b0100000".U) {
        io.uop := isSUB
      }.otherwise {
        io.uop := invalid
      }
    }.elsewhen(funct3 === "b100".U) {
      io.uop := Mux(funct7 === "b0000000".U, isXOR, invalid)
    }.elsewhen(funct3 === "b110".U) {
      io.uop := Mux(funct7 === "b0000000".U, isOR, invalid)
    }.elsewhen(funct3 === "b111".U) {
      io.uop := Mux(funct7 === "b0000000".U, isAND, invalid)
    }.elsewhen(funct3 === "b001".U) {
      io.uop := Mux(funct7 === "b0000000".U, isSLL, invalid)
    }.elsewhen(funct3 === "b101".U) {
      io.uop := MuxLookup(funct7, invalid, Seq(
        "b0000000".U -> isSRL,
        "b0100000".U -> isSRA
      ))
    }.elsewhen(funct3 === "b010".U) {
      io.uop := Mux(funct7 === "b0000000".U, isSLT, invalid)
    }.elsewhen(funct3 === "b011".U) {
      io.uop := Mux(funct7 === "b0000000".U, isSLTU, invalid)
    }.otherwise {
      io.uop := invalid
    }
  }.elsewhen(opcode === "b0010011".U) { // I-Type
    when(funct3 === "b000".U) {
      io.uop := isADDI
    }.otherwise {
      io.uop := invalid
    }
  }.elsewhen(opcode === "b1101111".U) { // JAL (Jump and Link)
    when(jump_internal(1, 0) === 0.U) { // Word-aligned check
      io.Jump_flag := 1.U
      io.Jumpaddress := jump_internal
      io.uop := isADDI
    }.otherwise {
      io.uop := invalid
    }
  }.elsewhen(opcode === "b1100011".U){
    io.Branchaddress := branch_address
    when(funct3 === "b100".U){
      io.uop := isBLT
    }.elsewhen(funct3 === "b110".U){
      io.uop := isBLTU
    }.elsewhen(funct3 === "b001".U){
      io.uop := isBNE
    }.elsewhen(funct3 === "b101".U){
      io.uop := isBGE
    }.elsewhen(funct3 === "b111".U){
      io.uop := isBGEU
    }
  }



  // Register File Requests
  io.regFileReq_A.addr := rs1
  io.regFileReq_B.addr := rs2

  // Operand Selection
  io.operandA := Mux(io.Jump_flag === 1.U, io.PC, io.regFileResp_A.data)
  io.operandB := Mux(io.Jump_flag === 1.U, 4.U,Mux((opcode ==="b0110011".U || opcode === "b1100011".U), io.regFileResp_B.data, Mux(opcode === "b0010011".U, imm.asUInt, 0.U)))

  // Register Destinations
  io.rs1 := rs1
  io.rs2 := rs2
  io.rd := io.instr(11, 7)
}
// -----------------------------------------
// Execute Stage
// -----------------------------------------

class EX extends Module {
  val io = IO(new Bundle {
    val uop       = Input(uopc())
    val operandA  = Input(UInt(32.W))
    val operandB  = Input(UInt(32.W))
    val Branchaddress = Input(UInt(32.W))
    val rd_in = Input(UInt(5.W))
    val aluResult = Output(UInt(32.W))
    val Branchtaken = Output(UInt(1.W))
    val Branchaddress_taken  = Output(UInt(32.W))
    val rd_out = Output(UInt(5.W))
    //val CurrentPC_IN = Input(UInt(32.W))
    //val BranchPC_OUT = Output(UInt(32.W))
    //val BranchTrue = Output(UInt(32.W))
  })

  val operandA = io.operandA
  val operandB = io.operandB
  val uop      = io.uop
  //val CurrentPC = io.CurrentPC_IN
  io.rd_out := io.rd_in

  //initiaise
  io.Branchtaken := 0.U
  io.aluResult := 0.U

  when(uop === isADDI) { 
      io.aluResult := operandA + operandB 
    }.elsewhen(uop === isADD) {                           
      io.aluResult := operandA + operandB 
    }.elsewhen(uop === isSUB) {  
      io.aluResult := operandA - operandB 
    }.elsewhen(uop === isXOR) {  
      io.aluResult := operandA ^ operandB 
    }.elsewhen(uop === isOR) {  
      io.aluResult := operandA | operandB 
    }.elsewhen(uop === isAND) {  
      io.aluResult := operandA & operandB 
    }.elsewhen(uop === isSLL) {  
      io.aluResult := operandA << operandB(4, 0) 
    }.elsewhen(uop === isSRL) {  
      io.aluResult := operandA >> operandB(4, 0) 
    }.elsewhen(uop === isSRA) {  
      io.aluResult := operandA >> operandB(4, 0)          // automatic sign extension, if SInt datatype is used
    }.elsewhen(uop === isSLT) {  
      io.aluResult := Mux(operandA<operandB, 1.U, 0.U)   // automatic sign extension, if SInt datatype is used
    }.elsewhen(uop === isSLTU) {
    io.aluResult := Mux(operandA < operandB, 1.U, 0.U)
    }.elsewhen(uop === isBLT) {
      io.Branchtaken := Mux(operandA.asSInt < operandB.asSInt, 1.U, 0.U)
      io.rd_out := 0.U
  }.elsewhen(uop === isBLTU){
      io.Branchtaken := Mux(operandA < operandB, 1.U, 0.U)
      io.rd_out := 0.U
    }.elsewhen(uop === isBGE) {
      io.Branchtaken := Mux(operandB.asSInt <= operandA.asSInt, 1.U, 0.U)
      io.rd_out := 0.U
    }.elsewhen(uop === isBGEU){
      io.Branchtaken := Mux(operandB <= operandA , 1.U, 0.U)
      io.rd_out := 0.U
    }.elsewhen(uop === isBNE){
      io.Branchtaken := Mux(operandA =/= operandB, 1.U , 0.U)
      io.rd_out := 0.U
    }.otherwise{
      io.aluResult := "h_FFFF_FFFF".U // = 2^32 - 1; self-defined encoding for invalid operation, value is unlikely to be reached in a regular arithmetic operation
    }
  io.Branchaddress_taken := io.Branchaddress

}

// -----------------------------------------
// Memory Stage
// -----------------------------------------

class MEM extends Module {
  val io = IO(new Bundle {

  })

  // No memory operations implemented in this basic CPU

}

// -----------------------------------------
// Writeback Stage
// -----------------------------------------

class WB extends Module {
  val io = IO(new Bundle {
    val regFileReq = Flipped(new regFileWriteReq) 
    val rd         = Input(UInt(5.W))
    val aluResult  = Input(UInt(32.W))
    val check_res  = Output(UInt(32.W))
  })

 io.regFileReq.addr  := io.rd
 io.regFileReq.data  := io.aluResult
 io.regFileReq.wr_en := io.aluResult =/= "h_FFFF_FFFF".U  // could depend on the current uopc, if ISA is extendet beyond R-type and I-type instructions

 io.check_res := io.aluResult

}


// -----------------------------------------
// IF-Barrier
// -----------------------------------------

class IFBarrier extends Module {
  val io = IO(new Bundle {
    val inInstr  = Input(UInt(32.W))
    val CurrentPC_IN = Input(UInt(32.W))
    val Jump_flush = Input(UInt(1.W)) // to be controlled by Predictor
    val Branchtaken = Input(UInt(1.W)) // to be controlled by Predictor
    val outInstr = Output(UInt(32.W))
    val CurrentPC_OUT = Output(UInt(32.W))
  })

  // Register definitions
  val instrReg = RegInit(0.U(32.W))  // Instruction register
  val PC_value = RegInit(0.U(32.W))  // PC register

  // On every clock cycle
  when(io.Jump_flush === 1.U || io.Branchtaken === 1.U) {
    // If Jump_flush is asserted, set instrReg to 9 (NOP or invalid instruction)
    instrReg := 9.U
  }.otherwise {
    // Otherwise, store the incoming instruction
    instrReg := io.inInstr
  }
  // Update the PC value (next clock cycle)
  PC_value := io.CurrentPC_IN

  // Output the current PC and instruction
  io.CurrentPC_OUT := PC_value
  io.outInstr := instrReg


}


// -----------------------------------------
// ID-Barrier
// -----------------------------------------

class IDBarrier extends Module {
  val io = IO(new Bundle {
    val inUOP       = Input(uopc())
    val inRD        = Input(UInt(5.W))
    val inRS1       = Input(UInt(5.W))
    val inRS2       = Input(UInt(5.W))
    val inOperandA  = Input(UInt(32.W))
    val inOperandB  = Input(UInt(32.W))
    val Branchaddress_in = Input(UInt(32.W))
    val Branchtaken = Input(UInt(1.W))    // to be controlled by Predictor
    //val CurrentPC_IN = Input(UInt(32.W))
    val outUOP      = Output(uopc())
    val outRD       = Output(UInt(5.W))
    val outRS1      = Output(UInt(5.W))
    val outRS2      = Output(UInt(5.W))
    val outOperandA = Output(UInt(32.W))
    val outOperandB = Output(UInt(32.W))
    val Branchadress_out = Output(UInt(32.W))
    //val CurrentPC_OUT = Output(UInt(32.W))
  })

  val uop      = Reg(uopc())
  val rd       = RegInit(0.U(5.W))
  val rs1      = RegInit(0.U(5.W))
  val rs2      = RegInit(0.U(5.W))
  val operandA = RegInit(0.U(32.W))
  val operandB = RegInit(0.U(32.W))
  val Branchaddress_reg = RegInit(0.U(32.W))
  //val PC_value = RegInit(0.U(32.W))
  io.outUOP := uop
  io.outRD := rd
  io.outRS1 := rs1
  io.outRS2 := rs2
  io.outOperandA := operandA
  io.outOperandB := operandB
  io.Branchadress_out := Branchaddress_reg
  when(io.Branchtaken === 1.U){
    uop:= isADDI
    operandA := 0.U
    operandB := 0.U
    rd := 0.U
  }.otherwise{
    uop := io.inUOP
    rd := io.inRD
    operandA :=  io.inOperandA
    operandB := io.inOperandB
  }

  rs2 := io.inRS2
  rs1 := io.inRS1
  Branchaddress_reg := io.Branchaddress_in

  /*io.outUOP := uop
  uop := io.inUOP
  io.outRD := rd
  rd := io.inRD
  io.outRS1 := rs1
  //io.CurrentPC_OUT := PC_value
  rs1 := io.inRS1
  io.outRS2 := rs2
  rs2 := io.inRS2
  io.outOperandA := operandA
  operandA := io.inOperandA
  io.outOperandB := operandB
  operandB := io.inOperandB
  //PC_value := io.CurrentPC_IN*/



}


// -----------------------------------------
// EX-Barrier
// -----------------------------------------

class EXBarrier extends Module {
  val io = IO(new Bundle {
    val inAluResult  = Input(UInt(32.W))
    val outAluResult = Output(UInt(32.W))
    val inRD         = Input(UInt(5.W))
    val outRD        = Output(UInt(5.W))
  })

  val aluResult = RegInit(0.U(32.W))
  val rd       = RegInit(0.U(5.W))

  io.outAluResult := aluResult
  aluResult       := io.inAluResult

  io.outRD := rd
  rd := io.inRD

}


// -----------------------------------------
// MEM-Barrier
// -----------------------------------------

class MEMBarrier extends Module {
  val io = IO(new Bundle {
    val inAluResult  = Input(UInt(32.W))
    val outAluResult = Output(UInt(32.W))
    val inRD         = Input(UInt(5.W))
    val outRD        = Output(UInt(5.W))
  })

  val aluResult = RegInit(0.U(32.W))
  val rd        = RegInit(0.U(5.W))

  io.outAluResult := aluResult
  aluResult       := io.inAluResult

  io.outRD := rd
  rd := io.inRD

}


// -----------------------------------------
// WB-Barrier
// -----------------------------------------

class WBBarrier extends Module {
  val io = IO(new Bundle {
    val inCheckRes   = Input(UInt(32.W))
    val outCheckRes  = Output(UInt(32.W))
  })

  val check_res   = RegInit(0.U(32.W))

  io.outCheckRes := check_res
  check_res      := io.inCheckRes
}


// -----------------------------------------
// Main Class
// -----------------------------------------

class HazardDetectionRV32Icore (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res = Output(UInt(32.W))
  })


  // Pipeline Registers
  val IFBarrier  = Module(new IFBarrier)
  val IDBarrier  = Module(new IDBarrier)
  val EXBarrier  = Module(new EXBarrier)
  val MEMBarrier = Module(new MEMBarrier)
  val WBBarrier  = Module(new WBBarrier)

  // Pipeline Stages
  val IF  = Module(new IF(BinaryFile))
  val ID  = Module(new ID)
  val EX  = Module(new EX)
  val MEM = Module(new MEM)
  val WB  = Module(new WB)

  /* 
    TODO: Instantiate the forwarding unit.
  */
  val frwd_unit = Module(new ForwardingUnit)


  //Register File
  val regFile = Module(new regFile)

  // Connections for IOs
  IF.io.JumpPC := ID.io.Jumpaddress
  IF.io.Jumpflush := ID.io.Jump_flag
  IF.io.BranchPC := EX.io.Branchaddress
  IF.io.Branchtaken := EX.io.Branchtaken
  IFBarrier.io.inInstr      := IF.io.instr
  IFBarrier.io.Jump_flush   := ID.io.Jump_flag //to be from predictor
  IFBarrier.io.CurrentPC_IN := IF.io.Computed_PC
  IFBarrier.io.Branchtaken   := EX.io.Branchtaken //to be from predictor


  ID.io.instr               := IFBarrier.io.outInstr
  ID.io.regFileReq_A        <> regFile.io.req_1
  ID.io.regFileReq_B        <> regFile.io.req_2
  ID.io.regFileResp_A       <> regFile.io.resp_1
  ID.io.regFileResp_B       <> regFile.io.resp_2
  ID.io.PC                  := IFBarrier.io.CurrentPC_OUT

  IDBarrier.io.inUOP        := ID.io.uop
  IDBarrier.io.inRD         := ID.io.rd
  IDBarrier.io.inRS1        := ID.io.rs1
  IDBarrier.io.inRS2        := ID.io.rs2
  IDBarrier.io.inOperandA   := ID.io.operandA
  IDBarrier.io.inOperandB   := ID.io.operandB
  IDBarrier.io.Branchaddress_in := ID.io.Branchaddress
  IDBarrier.io.Branchtaken := EX.io.Branchtaken //to be from predictor
  frwd_unit.io.rs1_addr := IDBarrier.io.outRS1
  frwd_unit.io.rs2_addr := IDBarrier.io.outRS2



  /* 
    TODO: Connect the I/Os of the forwarding unit 
  */

  /* 
    TODO: Implement MUXes to select which values are sent to the EX stage as operands
  */

  EX.io.uop := IDBarrier.io.outUOP
  EX.io.Branchaddress := IDBarrier.io.Branchadress_out
  EX.io.rd_in := IDBarrier.io.outRD
  EX.io.operandA := Mux(frwd_unit.io.operandA_frwd === 0.U,IDBarrier.io.outOperandA, Mux(frwd_unit.io.operandA_frwd === 2.U,MEMBarrier.io.outAluResult, EXBarrier.io.outAluResult))
  EX.io.operandB:= Mux(frwd_unit.io.operandB_frwd === 0.U,IDBarrier.io.outOperandB, Mux(frwd_unit.io.operandB_frwd === 2.U,MEMBarrier.io.outAluResult,EXBarrier.io.outAluResult))
  /*
    TODO: Connect operand inputs in EX stage to forwarding logic
  */
 // EX.io.operandA := 0.U // just there to make empty project buildable
 // EX.io.operandB := 0.U // just there to make empty project buildable

  EXBarrier.io.inRD         := EX.io.rd_out
  EXBarrier.io.inAluResult  := EX.io.aluResult
  frwd_unit.io.rd_addr_exbarrier := EXBarrier.io.outRD

  MEMBarrier.io.inRD        := EXBarrier.io.outRD
  MEMBarrier.io.inAluResult := EXBarrier.io.outAluResult
  frwd_unit.io.rd_addr_membarrier := MEMBarrier.io.outRD

  WB.io.rd                  := MEMBarrier.io.outRD
  WB.io.aluResult           := MEMBarrier.io.outAluResult
  WB.io.regFileReq          <> regFile.io.req_3

  WBBarrier.io.inCheckRes   := WB.io.check_res

  io.check_res              := WBBarrier.io.outCheckRes

}
