// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chisel3.util._


/** 
  * Half Adder Class 
  * 
  * Your task is to implement a basic half adder as presented in the lecture.
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.s
  */
class HalfAdder extends Module{
  
  val io = IO(new Bundle {
      /*
       * TODO: Define IO ports of a half adder as presented in the lecture
       */
       val a = Input (UInt(1.W))
       val b = Input (UInt(1.W))
       val s = Output (UInt(1.W))
       val c = Output (UInt(1.W))
      })
  /* 
   * TODO: Describe output behaviour based on the input values
   */
       io.s := io.a ^ io.b // sum is the XOR of inputs
       io.c := io.a & io.b // Carry is the AND of inputs

}

/** 
  * Full Adder Class 
  * 
  * Your task is to implement a basic full adder. The component's behaviour should 
  * match the characteristics presented in the lecture. In addition, you are only allowed 
  * to use two half adders (use the class that you already implemented) and basic logic 
  * operators (AND, OR, ...).
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FullAdder extends Module{

  val io = IO(new Bundle {
    /* 
     * TODO: Define IO ports of a half adder as presented in the lecture
     */
     val a = Input (UInt(1.W))
     val b = Input (UInt(1.W))
     val c = Input (UInt(1.W))
     val sum = Output (UInt(1.W))
     val carry = Output (UInt(1.W))
     })
     val sum1 = Wire(UInt(1.W))
     val carry1 = Wire(UInt(1.W))
     val carry2 = Wire(UInt(1.W))


  /* 
   * TODO: Instanciate the two half adders you want to use based on your HalfAdder class
   */
     val halfadder1 = Module(new HalfAdder())
     val halfadder2 = Module(new HalfAdder())
     //Connections half adder1
     halfadder1.io.a := io.a
     halfadder1.io.b := io.b
     sum1 := halfadder1.io.s
     carry1 := halfadder1.io.c

     //Connections half adder2
     halfadder2.io.a := sum1
     halfadder2.io.b := io.c
     carry2 := halfadder2.io.c


  /* 
   * TODO: Describe output behaviour based on the input values and the internal signals
   */
     io.sum := halfadder2.io.s
     io.carry := carry1| carry2
}

/** 
  * 4-bit Adder class 
  * 
  * Your task is to implement a 4-bit ripple-carry-adder. The component's behaviour should 
  * match the characteristics presented in the lecture.  Remember: An n-bit adder can be 
  * build using one half adder and n-1 full adders.
  * The inputs and the result should all be 4-bit wide, the carry-out only needs one bit.
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FourBitAdder extends Module{

  val io = IO(new Bundle {
    /* 
     * TODO: Define IO ports of a 4-bit ripple-carry-adder as presented in the lecture
     */
    val a, b = Input(UInt(4.W))
    val sum = Output(UInt(4.W))
    val carry = Output(UInt(1.W))
    })
    val tempcarry1 = Wire(UInt(1.W))
    val tempcarry2 = Wire(UInt(1.W))
    val tempcarry3 = Wire(UInt(1.W))
    val tempcarry4 = Wire(UInt(1.W))


  /* 
   * TODO: Instanciate the full adders and one half adderbased on the previously defined classes
   */
    val halfadder = Module(new HalfAdder())
    val fulladder1 = Module(new FullAdder())
    val fulladder2 = Module(new FullAdder())
    val fulladder3 = Module(new FullAdder())
  // Half Adder for the LSB
    halfadder.io.a := io.a(0)
    halfadder.io.b := io.b(0)
    val tempsum1 = halfadder.io.s
    tempcarry1 := halfadder.io.c
  // Full Adder for the second bit
    fulladder1.io.a := io.a(1)
    fulladder1.io.b := io.b(1)
    fulladder1.io.c := tempcarry1
    val tempsum2 = fulladder1.io.sum
    tempcarry2 := fulladder1.io.carry
  // Full Adder for the third bit
    fulladder2.io.a := io.a(2)
    fulladder2.io.b := io.b(2)
    fulladder2.io.c := tempcarry2
    val tempsum3 = fulladder2.io.sum
    tempcarry3 := fulladder2.io.carry
  // Full Adder for the MSB
    fulladder3.io.a := io.a(3)
    fulladder3.io.b := io.b(3)
    fulladder3.io.c := tempcarry3
    val tempsum4 = fulladder3.io.sum
    tempcarry4 := fulladder3.io.carry


  /* 
   * TODO: Describe output behaviour based on the input values and the internal 
   */
     io.carry  := tempcarry4
     io.sum  := Cat(tempsum4,tempsum3,tempsum2,tempsum1).asUInt
}
