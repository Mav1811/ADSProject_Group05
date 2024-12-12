package readserial

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

object ControllerState extends ChiselEnum {
  val START_TRANSFER, WAIT, RESET = Value
}

object CounterState extends ChiselEnum {
  val START_COUNT, COUNT, RESET = Value
}

class ControllerModule extends Module {
  val io = IO(new Bundle {
    val rst = Input(UInt(1.W))         // reset signal
    val serial_bus = Input(UInt(1.W))  // bus input
    val count_s = Input(UInt(4.W))     // Counter value
    val ready = Output(UInt(1.W))      // valid signal for output ready
    val start = Output(UInt(1.W))      // counter trigger
  })

  // Import the states from ControllerState
  import ControllerState._

  // Internal register for the FSM state
  val state = RegInit(START_TRANSFER) // Initial state is START_TRANSFER
  val out_ready = RegInit(0.U(1.W))  // Register for output ready
  val count_start = RegInit(0.U(1.W)) // Register to trigger the counter

  // Default assignments
  io.start := 0.U
  io.ready := 0.U

  // State machine logic using switch/case
  switch(state) {
    is(START_TRANSFER) {
      // Start the transfer by starting the counter
      when(io.serial_bus === 0.U && io.count_s === 0.U) {
        io.start := 1.U // Trigger the counter to start
        state := WAIT    // Transition to the WAIT state
      }.elsewhen(io.rst === 1.U){
        state := RESET
      }
    }
    is(WAIT) {
      io.start := 0.U
      // Wait until counter reaches 8
      when(io.count_s === 8.U) {
        io.ready := 1.U  // Set ready when 8 bits are received
        state := START_TRANSFER // Transition to START_TRANSFER state to begin a new transfer
      }.elsewhen(io.rst === 1.U) {
        state := RESET  // If reset signal is high, transition to RESET state
      }.otherwise {
        state := WAIT    // Stay in WAIT state
      }
    }
    is(RESET) {
      // Reset state
      io.ready := 0.U // Reset ready signal
      state := START_TRANSFER // Transition back to START_TRANSFER state to initiate the next transfer
    }
  }
}

class Counter extends Module {
  val io = IO(new Bundle {
    val rst = Input(UInt(1.W))     // reset signal
    val start = Input(UInt(1.W))   // start signal to trigger counting
    val count_s = Output(UInt(4.W)) // 3-bit counter output
  })

  import CounterState._

  // Internal registers
  val state = RegInit(START_COUNT)  // Initial state is START_COUNT
  val counter = RegInit(0.U(4.W))  // 3-bit counter, initialized to 0

  io.count_s := counter  // Output the counter value

  // State machine logic
  switch(state) {
    is(START_COUNT) {
      // Wait for the 'start' signal to begin counting
      when(io.start === 1.U) {
        state := COUNT  // Transition to COUNT state when 'start' signal is received
      }.elsewhen(io.rst === 1.U) {
        state := RESET  // Reset state if reset signal is high
      }
    }

    is(COUNT) {
      // Counting logic
      when(counter === 8.U) {
        counter := 0.U  // Reset counter after reaching 7
        state := START_COUNT // Change the state
      }.otherwise {
        counter := counter + 1.U  // Increment counter
      }
      // Reset condition
      when(io.rst === 1.U) {
        state := RESET  // Transition to RESET state if reset signal is high
      }
    }

    is(RESET) {
      // Reset the counter and go back to START_COUNT state
      counter := 0.U
      when(io.rst === 0.U) {
        state := START_COUNT  // Go back to START_COUNT when reset is released (0)
      }
    }
  }
}

class ShiftRegister extends Module{
  val io = IO(new Bundle {
    val serial_bus = Input(UInt(1.W))
    val parallel_out = Output(UInt(8.W))
  })

  val internal_reg = RegInit(0.U(8.W))
  io.parallel_out := 0.U

  // Functionality
  internal_reg := Cat(io.serial_bus, internal_reg(7, 1)) // Shift right and insert new serial bit at MSB
  io.parallel_out := internal_reg
}

class ReadSerial extends Module {
  val io = IO(new Bundle {
    val reset_n = Input(UInt(1.W))
    val rxd = Input(UInt(1.W))
    val valid = Output(UInt(1.W))
    val data = Output(UInt(8.W))
  })

  // Declare wires
  val counter_value = Wire(UInt(4.W))
  val counter_start = Wire(UInt(1.W))

  // Registers to avoid combinational loop
  // val counter_reg = RegInit(0.U(3.W)) // Register for counter_end
  // val counter_start_reg = RegInit(0.U(1.W)) // Register for counter_start

  // Instantiate modules
  val controller = Module(new ControllerModule())
  val counter = Module(new Counter())
  val shiftRegister = Module(new ShiftRegister())

  io.valid := 0.U
  io.data := 0.U

  // Update the registers with the wire values after each clock cycle
  //counter_reg := counter_value
  //counter_start_reg := counter_start

  // Controller connections
  controller.io.rst := io.reset_n
  controller.io.serial_bus := io.rxd
  controller.io.count_s := counter_value
  io.valid := controller.io.ready
  counter_start := controller.io.start

  // Counter connections
  counter.io.rst := io.reset_n
  counter.io.start := counter_start
  counter_value := counter.io.count_s

  // Shift register connections
  shiftRegister.io.serial_bus := io.rxd
  io.data := shiftRegister.io.parallel_out

  // when(io.reset_n === 1.U) {
  //  counter_reg := 0.U
  //  counter_start_reg := 0.U
  //}
}