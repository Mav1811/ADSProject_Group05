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
 * Full adder tester
 * Use the truth table from the exercise sheet to test all possible input combinations and the corresponding results exhaustively
 */
class FullAdderTester extends AnyFlatSpec with ChiselScalatestTester {

  "FullAdder" should "work" in {
    test(new FullAdder).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (a <- 0 to 1) {
        for (b <- 0 to 1) {
          for (c <- 0 to 1) {

            val s0 = a ^ b ^ c
            val c0 = (a & b) | (a & c) | (b & c)

            dut.io.a.poke(a.U) // input a is set to value a
            dut.io.b.poke(b.U)
            dut.io.c.poke(c.U) // input b is set to value b
            dut.io.carry.expect(c0.U) // move one clock cycle ahead
            dut.io.sum.expect(s0.U)

          }
        }
      }
    }
  }
}
