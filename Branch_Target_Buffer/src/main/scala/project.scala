package btb

import chisel3._
import chisel3.util._

class BTBEntry extends Bundle {
  val valid = UInt(1.W)
  val tag = UInt(27.W) // Assuming 24-bit tag (32-bit PC - 3-bit index - 5-bit offset)
  val target = UInt(32.W)
  val predictor = UInt(2.W) // 2-bit saturating counter
}

class FSM extends Module {
  val io = IO(new Bundle {
    val currentState = Input(UInt(2.W))
    val mispredict   = Input(Bool())
    val nextState    = Output(UInt(2.W))
  })
  // Next state logic based on misprediction
  io.nextState := MuxCase(0.U, Array(
    (io.currentState === 0.U) -> Mux(io.mispredict, 1.U, 0.U),
    (io.currentState === 1.U) -> Mux(io.mispredict, 2.U, 0.U),
    (io.currentState === 2.U) -> Mux(io.mispredict, 3.U, 2.U),
    (io.currentState === 3.U) -> Mux(io.mispredict, 0.U, 2.U)
  ))
}

class BTB extends Module {
  val io = IO(new Bundle {
    val PC = Input(UInt(32.W))           // Changed 'pc' to 'PC'
    val update = Input(UInt(1.W))
    val updatePC = Input(UInt(32.W))
    val updateTarget = Input(UInt(32.W))
    val mispredicted = Input(UInt(1.W))
    val valid = Output(UInt(1.W))
    val target = Output(UInt(32.W))
    val predictedTaken = Output(UInt(1.W))
  })

  val btb = RegInit(VecInit(Seq.fill(8)(VecInit(Seq.fill(2)(0.U.asTypeOf(new BTBEntry))))))
  val lru = RegInit(VecInit(Seq.fill(8)(0.U(1.W)))) // LRU bit for each set



  val fsm = Module(new FSM)
  fsm.io.currentState := 1.U
  fsm.io.mispredict := false.B
  when(io.update === 1.U) {                 // writing into buffer
    val updateIndex = io.updatePC(4, 2)
    val updateTag = io.updatePC(31, 5)
    val replaceWay = lru(updateIndex) // Select way to replace


    when(btb(updateIndex)(0).valid.asBool && btb(updateIndex)(0).tag === updateTag){     // write on block(index)(0)
      fsm.io.currentState := btb(updateIndex)(0).predictor
      fsm.io.mispredict := io.mispredicted.asBool
      btb(updateIndex)(0).target := io.updateTarget
      btb(updateIndex)(0).predictor := fsm.io.nextState
      btb(updateIndex)(0).valid := 1.U
      lru(updateIndex) := 1.U // Mark other way as LRU
    }.elsewhen(btb(updateIndex)(1).valid.asBool && btb(updateIndex)(1).tag === updateTag) {    // write on block(index)(1)
      fsm.io.currentState := btb(updateIndex)(1).predictor
      fsm.io.mispredict := io.mispredicted.asBool
      btb(updateIndex)(1).target := io.updateTarget
      btb(updateIndex)(1).predictor := fsm.io.nextState
      lru(updateIndex) := 0.U // Mark other way as LRU
      btb(updateIndex)(1).valid := 1.U
    }.otherwise {                                                                            // write if there is no match
      btb(updateIndex)(replaceWay).valid := 1.U
      btb(updateIndex)(replaceWay).tag := updateTag
      btb(updateIndex)(replaceWay).target := io.updateTarget
      fsm.io.mispredict := io.mispredicted.asBool()
      btb(updateIndex)(replaceWay).predictor := fsm.io.nextState
      lru(updateIndex) := replaceWay ^ 1.U
    }
  }
  val index = io.PC(4, 2) // Extract index bits (8 sets)
  val tag = io.PC(31, 5)  // Extract tag
  val predictedout = Wire(UInt(2.W))
  predictedout := 1.U

   when((io.update === 1.U) && io.PC === io.updatePC) {                      // forwarding when pc is same as update pc
      io.valid := io.update
      io.target := io.updateTarget
     //predictedout := btb(index)(0).predictor
     io.predictedTaken := fsm.io.nextState(1) // most significant bit representing prediction
      //lru(index) := 0.U
    }.elsewhen((btb(index)(0).valid === 1.U) && btb(index)(0).tag === tag) {     // read in block(index)(0)
    io.valid := btb(index)(0).valid
    io.target := btb(index)(0).target
     predictedout := btb(index)(0).predictor
    io.predictedTaken := predictedout(1)// most significant bit representing prediction
    lru(index) := 1.U
  }.elsewhen(btb(index)(1).valid.asBool && btb(index)(1).tag === tag) {      // read in block(index)(1)
    io.valid := btb(index)(1).valid
    io.target := btb(index)(1).target
     predictedout := btb(index)(1).predictor
     io.predictedTaken := predictedout(1) // most significant bit representing prediction
    lru(index) := 0.U
  }.otherwise {                                                             // read value when there is no match in btb
    io.valid := 0.U
    io.target := 0.U
    io.predictedTaken := 0.U
  }


}