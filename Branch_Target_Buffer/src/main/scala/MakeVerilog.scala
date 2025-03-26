// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package makeverilog

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import btb._


object Verilog_Gen extends App {
  emitVerilog(new BTB(), Array("--target-dir", "generated-src"))
  //emitVerilog(new HalfAdder(), Array("--target-dir", "generated-src"))
  //emitVerilog(new FullAdder(), Array("--target-dir", "generated-src"))
  //emitVerilog(new FourBitAdder(), Array("--target-dir", "generated-src"))
  //emitVerilog(new ReadSerialAdder(), Array("--target-dir", "generated-src"))
}
