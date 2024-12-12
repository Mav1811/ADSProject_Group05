// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


/**
 * Half adder tester
 * Use the truth table from the exercise sheet to test all possible input combinations and the corresponding results exhaustively
 */
class HalfAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "HalfAdder" should "work" in {
    test(new HalfAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (a <- 0 to 1) {
        for (b <- 0 to 1) {

          val s = a ^ b
          val c = a & b

          dut.io.a.poke(a.U) // input a is set to value a
          dut.io.b.poke(b.U) // input b is set to value b
          dut.io.c.expect(c.U) // move one clock cycle ahead
          dut.io.s.expect(s.U) // see if output c matches expected result
        }

        /*dut.io.a.poke(...)
         *dut.io.b.poke(...)
         *dut.io.ci.poke(...)
         *dut.io.s.expect(...)
         *dut.io.co.expect(...)
         *...
         *TODO: Insert your test cases
         */

      }
    }
  }
}
