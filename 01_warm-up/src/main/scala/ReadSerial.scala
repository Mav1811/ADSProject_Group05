// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chisel3.util._


/** Controller class */
class Controller extends Module {

  val io = IO(new Bundle {
    val rst = Input(UInt(1.W))         // reset signal
    val serial_bus = Input(UInt(1.W))  // bus input
    val end = Input(UInt(1.W))         // end of 8bits from counter
    val ready = Output(UInt(1.W))      // valid signal for output ready
    val start = Output(UInt(1.W))      // counter trigger
  })

  // Internal registers for the state machine and control signals
  val out_ready = RegInit(0.U(1.W))  // Register for output ready
  val count_start = RegInit(0.U(1.W)) // Register to trigger the counter

  // State machine functionality
  when(io.rst === 1.U) {
    // Reset condition
    io.ready := 0.U
    io.start := 0.U
    out_ready := 0.U
  } .elsewhen(io.serial_bus === 0.U && out_ready === 1.U) {
    // Condition where serial_bus is 0 and out_ready is set
    io.start := 1.U
    out_ready := 0.U
  } .elsewhen(io.end === 1.U) {
    // End of 8 bits
    out_ready := 1.U
    io.ready := 1.U
  } .otherwise {
    // Default case
    io.start := 0.U
    io.ready := 0.U
    out_ready := 0.U //  to avoid unintended active state
  }
}

/** counter class */
class Counter extends Module{
  
  val io = IO(new Bundle {

    val rst = Input(UInt(1.W))
    val start = Input(UInt(1.W))
    val end = Output(UInt(1.W))
    /* 
     * TODO: Define IO ports of a the component as stated in the documentation
     */
    })
  // internal variables
  /* 
   * TODO: Define internal variables (registers and/or wires), if needed
   */
  val counter = RegInit(0.U(3.W))

  // state machine
  /* 
   * TODO: Describe functionality if the counter as a state machine
   */
  when(io.rst === 1.U)
  {
    // reset state
    counter = 0.U
    io.end := 0.U
  }.elsewhen(counter == 7.U )
  {
    //if counter = 8 , output is ready
    io.end := 1.U
    counter :=0.U
  }.elsewhen(io.start === 1.U){
    // start of the counter
    counter := counter + 1.U
  }.otherwise{
    //default state
    io.end := 0.U
  }

}

/** shift register class */
class ShiftRegister extends Module{
  
  val io = IO(new Bundle {
    /* 
     * TODO: Define IO ports of a the component as stated in the documentation
     */
    val serial_bus = Input(UInt(1.W))
    val parallel_out = Output(UInt(8.W))
    })
    val internal_reg = RegInit(0.U(8.W))
  // internal variables
  /* 
   * TODO: Define internal variables (registers and/or wires), if needed
   */

  // functionality
  /* 
   * TODO: Describe functionality if the shift register
   */
  internal_reg:= Cat(io.serial_bus, internal_reg(7, 1)) // Shift right and insert new serial bit at MSB
  parllel_out := internal_reg

}

/** 
  * The last warm-up task deals with a more complex component. Your goal is to design a serial receiver.
  * It scans an input line (“serial bus”) named rxd for serial transmissions of data bytes. A transmission 
  * begins with a start bit ‘0’ followed by 8 data bits. The most significant bit (MSB) is transmitted first. 
  * There is no parity bit and no stop bit. After the last data bit has been transferred a new transmission 
  * (beginning with a start bit, ‘0’) may immediately follow. If there is no new transmission the bus line 
  * goes high (‘1’, this is considered the “idle” bus signal). In this case the receiver waits until the next 
  * transmission begins. The outputs of the design are an 8-bit parallel data signal and a valid signal. 
  * The valid signal goes high (‘1’) for one clock cycle after the last serial bit has been transmitted, 
  * indicating that a new data byte is ready.
  */
class ReadSerial extends Module{
  
  val io = IO(new Bundle {
    /* 
     * TODO: Define IO ports of a the component as stated in the documentation
     */
    val reset_n = Input(UInt(1.W))
    val rxd = Input(UInt(1.W))
    val valid = Output(UInt(1.W))
    val data = Output(UInt(8.W))
    })
  val counter_end = Wire(UInt(1.W))
  val counter_start = Wire(UInt(1.W))
  // instanciation of modules
  /* 
   * TODO: Instanciate the modules that you need
   */
  val Controller = Module (new Controller())
  val Counter = Module(new Counter())
  val ShiftRegister = Module( new ShiftRegister())
  // connections between modules
  /* 
   * TODO: connect the signals between the modules
   */
  //controller connections
  Controller.io.rst := io.reset_n
  Controller.io.serial := io.rxd
  Controller.io.end := counter_end
  io.valid := Controller.io.ready
  counter_start := Controller.io.start

  // Counter connections
  Counter.io.rst := io.reset_n
  Counter.io.start:= counter_start
  counter_end := Counter.io.end

  // Shift register connections
  ShiftRegister.io.serial_bus := io.rxd
  io.data := ShiftRegister.io.parallel_out

}
