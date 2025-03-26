package btb
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class btbTester extends AnyFlatSpec with ChiselScalatestTester {

  "btb" should "work" in {
    test(new BTB).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val list_PCs_TarAddrs = List("h000A0000".U, "h000B0000".U, "h000A0020".U, "h000B0020".U, "h000A0040".U, "h000B0040".U,  //3 entries for set 0
        "h000A0004".U, "h000B0004".U, "h000A0024".U, "h000B0024".U, "h000A0044".U, "h000B0044".U, //3 entries for set 1
        "h000A0008".U, "h000B0008".U, "h000A0028".U, "h000B0028".U, "h000A0048".U, "h000B0048".U, //3 entries for set 2
        "h000A000C".U, "h000B000C".U, "h000A002C".U, "h000B002C".U, "h000A004C".U, "h000B004C".U, //3 entries for set 3
        "h000A0010".U, "h000B0010".U, "h000A0030".U, "h000B0030".U, "h000A0050".U, "h000B0050".U, //3 entries for set 4
        "h000A0014".U, "h000B0014".U, "h000A0034".U, "h000B0034".U, "h000A0054".U, "h000B0054".U, //3 entries for set 5
        "h000A0018".U, "h000B0018".U, "h000A0038".U, "h000B0038".U, "h000A0058".U, "h000B0058".U, //3 entries for set 6
        "h000A001C".U, "h000B001C".U, "h000A003C".U, "h000B003C".U, "h000A005C".U, "h000B005C".U) //3 entries for set 7
      // 8 loops for 8 sets
      // PC_i, Tar_Add_i are the PC and corresponding target address (there are 3 entries for testing each set)
      for(element <- list_PCs_TarAddrs.grouped(6))
      {
        val PC_0      = element(0)
        val Tar_Add_0 = element(1)
        val PC_1      = element(2)
        val Tar_Add_1 = element(3)
        val PC_2      = element(4)
        val Tar_Add_2 = element(5)
        // Test case 0: No entry is written when update is false
        // write first instruction to btb with update = false
        dut.io.update.poke(0.U)               // set updating btb
        dut.io.updatePC.poke(PC_0)            // PC should be in the set
        dut.io.updateTarget.poke(Tar_Add_0)   // Target address of ins
        dut.io.mispredicted.poke(0.U)         // suppose no mispredict

        dut.clock.step(1)

        // check if first instruction is available
        dut.io.PC.poke(PC_0)                  // set PC with written entry before
        dut.clock.step(1)                     // wait 1 cycle to get outputs of btb
        dut.io.valid.expect(0.U)              // check valid

        // Test case 1: Test outputs: valid, target correctly after entry is written
        // write first instruction to btb
        dut.io.update.poke(1.U)               // set updating btb
        dut.io.updatePC.poke(PC_0)            // PC should be in the set
        dut.io.updateTarget.poke(Tar_Add_0)   // Target address of ins
        dut.io.mispredicted.poke(0.U)         // suppose no mispredict

        dut.clock.step(1)                     // wait 1 cycle for updating btb
        dut.io.update.poke(0.U)               // not allow update

        // check if first instruction is available
        dut.io.PC.poke(PC_0)                  // set PC with written entry before
        dut.clock.step(1)                     // wait 1 cycle to get outputs of btb
        dut.io.valid.expect(1.U)              // check valid
        dut.io.target.expect(Tar_Add_0)       // check Target address

        // Test case 2: check the proper transition of FSM
        // run instruction at second time with no mispredict to ensure btb is in strong state
        dut.io.update.poke(1.U)               // set updating btb
        dut.io.updatePC.poke(PC_0)            // PC should be in set 0
        dut.io.updateTarget.poke(Tar_Add_0)   // Target address of ins
        dut.io.mispredicted.poke(0.U)         // suppose no mispredict
        dut.io.PC.poke(PC_0)                  // set PC with PC_0
        dut.clock.step(1)
        dut.io.update.poke(0.U)               // not allow update

        if(dut.io.predictedTaken.peek.litValue == 0) {
          // btb is in strongNotTaken, run instruction for 2 times with mispredict, then btb should be in strongTaken
          dut.clock.step(1)
          dut.io.mispredicted.poke(1.U)         // mispredict
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(0.U)     // btb is in weakNotTaken

          dut.clock.step(1)
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(1.U)     // btb is in strongTaken

          // run instruction for 2 times more with mispredict, then btb should be in strongNotTaken
          dut.clock.step(1)
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(1.U)     // btb is in weakTaken

          dut.clock.step(1)
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(0.U)     // btb is in strongNotTaken
        } else{
          // btb is in strongTaken, run instruction for 2 times with mispredict, then btb should be in strongNotTaken
          dut.clock.step(1)
          dut.io.mispredicted.poke(1.U)         // mispredict
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(1.U)     // btb is in weakTaken

          dut.clock.step(1)
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(0.U)     // btb is in strongNotTaken

          // run instruction for 2 times more with mispredict, then btb should be in strongTaken
          dut.clock.step(1)
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(0.U)     // btb is in weakNotTaken

          dut.clock.step(1)
          dut.io.update.poke(1.U)               // set updating btb
          dut.clock.step(1)
          dut.io.update.poke(0.U)               // not allow update
          dut.io.predictedTaken.expect(1.U)     // btb is in strongTaken
        }

        // Test case 3: correct eviction behavior
        // write the second entry to the set
        // and check if btb can write and read at the same time at the same site
        dut.clock.step(1)
        dut.io.PC.poke(PC_1)                    // set PC with PC_1
        dut.clock.step(1)
        dut.io.update.poke(1.U)                 // set updating btb
        dut.io.updatePC.poke(PC_1)              // PC should be in the set
        dut.io.updateTarget.poke(Tar_Add_1)     // Target address of ins
        dut.io.mispredicted.poke(0.U)           // suppose no mispredict
        dut.io.valid.expect(1.U)                // valid should be true at the same clock when entry is written
        dut.io.target.expect(Tar_Add_1)         // target should be outputed at the same clock when entry is written
        dut.clock.step(1)
        dut.io.update.poke(0.U)                 // not allow update
        dut.io.valid.expect(1.U)                // valid is checked again
        dut.io.target.expect(Tar_Add_1)         // target is checked again

        // write the third entry to the set
        dut.clock.step(1)
        dut.io.update.poke(1.U)               // set updating btb
        dut.io.updatePC.poke(PC_2)            // PC should be in the set
        dut.io.updateTarget.poke(Tar_Add_2)   // Target address of ins
        dut.io.mispredicted.poke(0.U)         // suppose no mispredict

        dut.clock.step(1)
        dut.io.update.poke(0.U)               // not allow update
        dut.io.PC.poke(PC_2)                  // set PC with written entry before
        dut.clock.step(1)                     // wait 1 cycle to get outputs of btb
        dut.io.valid.expect(1.U)              // check valid
        dut.io.target.expect(Tar_Add_2)       // check Target address

        // check the first entry, it should be evicted already
        dut.clock.step(1)
        dut.io.PC.poke(PC_0)                  // set PC with least used instruction address in the set in btb
        dut.clock.step(1)
        dut.io.valid.expect(0.U)              // it should be evicted
        dut.clock.step(1)
      }
    }
    }
}
