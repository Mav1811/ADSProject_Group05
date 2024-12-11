// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/**
 *read serial tester
 */
class ReadSerialTester extends AnyFlatSpec with ChiselScalatestTester {

  "ReadSerial" should "work" in {
    test(new ReadSerial).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Reset the device under test (DUT)
      dut.io.reset_n.poke(0.U) // Assert reset
      dut.clock.step(1)         // Allow time for reset
      dut.io.reset_n.poke(1.U) // Deassert reset
      dut.clock.step(1) // Allow some time to come out of reset

      // test case 1
      dut.io.reset_n.poke(0.U)
      dut.io.rxd.poke(0.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // transmission started and 8 bits transmitted
      dut.clock.step(1)
      dut.io.rxd.poke(0.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      // output check
      dut.io.valid.expect(1.B)
      dut.io.data.expect("b10111111".U)
      // continous transmission without idle time
      dut.io.rxd.poke(0.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // transmission started and 8 bits transmitted
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      // output check
      dut.io.valid.expect(1.B)
      dut.io.data.expect("b11111111".U)
      // continous high signal no transmission
      dut.io.rxd.poke(1.U)
      dut.clock.step(10)
      dut.io.valid.expect(0.B)
      dut.io.data.expect(0.U)
      // incomplete transmission
      dut.io.rxd.poke(0.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(0.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)
      dut.io.rxd.poke(1.U)
      // output check
      dut.io.valid.expect(0.B)
      dut.io.data.expect(0.U)
      //
    }
  }
}
