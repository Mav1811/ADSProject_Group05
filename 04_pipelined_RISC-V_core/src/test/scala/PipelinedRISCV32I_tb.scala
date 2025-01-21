// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 12/19/2023 by Tobias Jauch (@tojauch)

package PipelinedRV32I_Tester

import chisel3._
import chiseltest._
import PipelinedRV32I._
import org.scalatest.flatspec.AnyFlatSpec

class PipelinedRISCV32ITest extends AnyFlatSpec with ChiselScalatestTester {

  "PipelinedRV32I_Tester" should "work" in {
    test(new PipelinedRV32I("src/test/programs/BinaryFile")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      /*
       * TODO: Insert your testcases from the previous assignments and adapt them for the pipelined core
       */
      dut.clock.setTimeout(0)
      dut.clock.step(5)
      dut.io.result.expect(0.U)             // ADDI x0, x0,  0
      dut.clock.step(1)
      dut.io.result.expect(4.U)             // ADDI x1, x0,  4
      dut.clock.step(1)
      dut.io.result.expect(5.U)             // ADDI x2, x0,  5
      dut.clock.step(4)                     // NOPx3; OUR INSTRUCTION
      dut.io.result.expect(9.U)             // ADD  x3, x1, x2
      dut.clock.step(4)
      dut.io.result.expect("hFFFFFFFC".U)   // SUB  x4, x2, x3
      dut.clock.step(4)
      dut.io.result.expect(8.U)             // AND  x5, x3, x4
      dut.clock.step(4)
      dut.io.result.expect("hFFFFFFFC".U)   // OR   x6, x4, x5
      dut.clock.step(4)
      dut.io.result.expect("hFFFFFFF4".U)   // XOR  x7, x5, x6
      dut.clock.step(4)
      dut.io.result.expect("hFFFFFFC4".U)   // ADDI x7, x7,-48
      dut.clock.step(4)
      dut.io.result.expect(1.U)             // SLTU x8, x3, x7
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // SLT x9, x3, x7
      dut.clock.step(4)
      dut.io.result.expect("hFFFFFFC4".U)   // SLL x10, x7, x9
      dut.clock.step(1)
      dut.io.result.expect(4.U)             // SRL x11, x3, x8
      dut.clock.step(4)
      dut.io.result.expect("hFFFFFFFC".U)   // SRA x12, x10, x11
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // SLTI x13, x2, 5
      dut.clock.step(1)
      dut.io.result.expect(0.U)             // SLTIU x14, x4, 10
      dut.clock.step(1)
      dut.io.result.expect(8.U)             // ANDI x15, x5, 10
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFFFE".U)   // ORI x16, x6, -2
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFFD0".U)   // XORI X17, X7, 20
      dut.clock.step(2)
      dut.io.result.expect(32.U)            // SLLI X18, X15, 2
      dut.clock.step(4)
      dut.io.result.expect(0.U)             // SRLI X19, X18, 16
      dut.clock.step(2)
      dut.io.result.expect(2.U)             // SRAI x20, x1, 1
      dut.clock.step(1)
      dut.io.result.expect("hFFFFFFFF".U)   // Default case: hFFFFFFFF
    }
  }
}

