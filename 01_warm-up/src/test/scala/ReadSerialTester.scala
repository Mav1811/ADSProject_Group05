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
      // Normal Transmission
      dut.io.reset_n.poke(0.U)
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



      // Reset Case during transmission
      dut.io.reset_n.poke(1.U) // Perform reset
      dut.io.rxd.poke(0.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(0.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      dut.io.rxd.poke(1.U) // Transmission should not occur due to reset
      dut.clock.step(1)
      // output check

      // Release reset and check if valid output goes low
      dut.io.valid.expect(0.B) // Output is invalid after reset


      //start of transmission only when the staring bit = 0
      dut.io.reset_n.poke(0.U)
      dut.io.rxd.poke(1.U)
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
      dut.io.valid.expect(0.B)

      //two transmissions
      dut.io.reset_n.poke(0.U)
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
      dut.io.valid.expect(1.B)
      dut.io.data.expect("b11111111".U)
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
      // output check




    }
    }
  }