// ADS I Class Project
// Multi-Cycle RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 12/19/2023 by Tobias Jauch (@tojauch)

package MultiCycleRV32I_Tester

import chisel3._
import chiseltest._
import MultiCycleRV32I._
import org.scalatest.flatspec.AnyFlatSpec

class MultiCycleRISCV32ITest extends AnyFlatSpec with ChiselScalatestTester {

  "MultiCycleRV32I_Tester" should "work" in {
    test(new MultiCycleRV32I("src/test/programs/BinaryFile")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      /*
       * TODO: Insert testcases from assignment 2 and adapt them for the multi-cycle core
       */
      dut.clock.setTimeout(0)
      dut.clock.step(4)
//      dut.io.result.expect(4.U)     // ADDI x0, x0, 0
      dut.clock.step(5)
      dut.io.result.expect(4.U)     // ADDI x1, x0, 4
      dut.clock.step(5)
      dut.io.result.expect(5.U)     // ADDI x2, x0, 5
      dut.clock.step(5)
      dut.io.result.expect(9.U)     // ADD x3, x1, x2
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFFC".U)
      dut.clock.step(5)
      dut.io.result.expect(8.U)
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFFC".U)
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFF4".U)
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFC4".U)
      dut.clock.step(5)
      dut.io.result.expect(1.U)
      dut.clock.step(5)
      dut.io.result.expect(0.U)
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFC4".U)
      dut.clock.step(5)
      dut.io.result.expect(4.U)
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFFC".U)
      dut.clock.step(5)
      dut.io.result.expect(0.U)
      dut.clock.step(5)
      dut.io.result.expect(0.U)
      dut.clock.step(5)
      dut.io.result.expect(8.U)
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFFE".U)
      dut.clock.step(5)
      dut.io.result.expect("hFFFFFFD0".U)
      dut.clock.step(5)
      dut.io.result.expect(32.U)
      dut.clock.step(5)
      dut.io.result.expect(0.U)
      dut.clock.step(5)
      dut.io.result.expect(2.U) // SRAI X20, X17, 2
      dut.clock.step(5)

      dut.io.result.expect(0.U)

    }
  }
}

