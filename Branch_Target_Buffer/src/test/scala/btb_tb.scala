package btb
import chisel3._
import chiseltest._
// Ensure this is correct if `BTB` and `btb_pkg` are defined elsewhere
import org.scalatest.flatspec.AnyFlatSpec

class BTBTest extends AnyFlatSpec with ChiselScalatestTester {

  "BTB_Tester" should "work" in {
    test(new BTB).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // Initialize inputs to the BTB
      dut.clock.setTimeout(0)  // Remove timeout

      var PC_1 = "hFBCD0000"
      var PC_2 = "hABCF0000"
      var PC_3 = "hABFF0000"


      // Test Case 1: Write to the BTB and check valid entry
      dut.io.PC.poke(PC_1.U)
      dut.io.update.poke(0.U)  // No update initially
      dut.io.updatePC.poke(0.U)
    //  dut.io.updateTarget.poke(0.U)
    //  dut.io.mispredicted.poke(0.U)

      dut.clock.step(1)
      dut.io.update.poke(0.U)
      dut.io.valid.expect(0.U)  // No valid entry yet

      // Write the first instruction
      dut.io.update.poke(1.U)  // Now allow updates to the BTB
      dut.io.updatePC.poke(PC_1.U)  // Writing for PC "hABCD0000"
      dut.io.updateTarget.poke(10.U)  // Target address 10
      dut.io.mispredicted.poke(1.U)  // Simulate a misprediction

      dut.clock.step(1)

      // Checking if the entry is valid and the target is correctly updated
      dut.io.update.poke(0.U)
      dut.io.PC.poke(PC_1.U)
      dut.clock.step(1)
      dut.io.valid.expect(1.U)  // BTB entry should now be valid
      dut.io.target.expect(10.U)  // Target should be 10
      dut.io.predictedTaken.expect(1.U)  // initial value is 01 - mispredicted - current state will be 10-



      // Reading and writing at the same PC address - forwarding logic in BTB
      dut.io.PC.poke(PC_2.U)
      dut.io.update.poke(1.U)  // Writing to the BTB at the same PC
      dut.io.updatePC.poke(PC_2.U)
      dut.io.updateTarget.poke(30.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)

      dut.io.valid.expect(1.U)
      dut.io.target.expect(30.U)
      dut.io.predictedTaken.expect(1.U)
      dut.io.update.poke(0.U)
      //_________ LRU_________//
   // Now we are reading the pc_1 and lru is toogled and now points to pc_2
      dut.clock.step(1)
      dut.io.PC.poke(PC_1.U)    // toggle the LRU bit
      dut.clock.step(1)
      dut.io.valid.expect(1.U)
      dut.io.target.expect(10.U)
      dut.io.predictedTaken.expect(1.U)
      dut.clock.step(1)

      // we are now updating pc_3 so it will replace pc_2

      dut.io.update.poke(1.U)
      dut.io.updatePC.poke(PC_3.U)
      dut.io.updateTarget.poke(40.U)
      dut.io.mispredicted.poke(0.U)

      dut.clock.step(1)
      dut.io.update.poke(0.U)
      dut.io.PC.poke(PC_2.U)  // This entry should be evicted
      dut.clock.step(1)
      dut.io.valid.expect(0.U)  // Entry for pc_2 should no longer be valid

      dut.clock.step(1)
      dut.io.PC.poke(PC_1.U)
      dut.clock.step(1)
      dut.io.valid.expect(1.U)     // should be getting valid for pc_1 and pc_3
      dut.io.target.expect(10.U)
      dut.io.predictedTaken.expect(1.U)

      dut.clock.step(1)
      dut.io.update.poke(0.U)
      dut.io.PC.poke(PC_3.U)
      dut.clock.step(1)
      dut.io.valid.expect(1.U)
      dut.io.target.expect(40.U)
      dut.io.predictedTaken.expect(0.U)

      // Step 4: FSM Test - State Transitions

      // Test FSM transition from strongNotTaken → weakNotTaken → strongTaken same pc value is used

      dut.io.PC.poke("hABCD0000".U)    // now starts at this pc
      dut.io.update.poke(1.U)
      dut.io.updatePC.poke("hABCD0000".U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.mispredicted.poke(0.U)        // intially pc is mispredicted is false so will be start in state strongNOtTaken

      dut.clock.step(1)
      dut.io.valid.expect(1.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.predictedTaken.expect(0.U)  // verified pc state strongNotTaken initially- 00

      // Step 1: First misprediction should move to weakNotTaken
      dut.io.mispredicted.poke(1.U)     // now changes to weak not taken
      dut.clock.step(1)
      dut.io.update.poke(0.U)
      dut.io.predictedTaken.expect(0.U)  // weakNotTaken - 01
      dut.clock.step(1)

      // Step 2: Second misprediction should move to strongTaken
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)     // now changes to  StrongTaken
      dut.clock.step(1)
      dut.io.update.poke(0.U)
      dut.io.predictedTaken.expect(1.U)  // current state -10- strong taken

      // Step 3 : Third misprediction should move to weakTaken
      dut.io.PC.poke("hABCD0000".U)
      dut.io.update.poke(1.U)
      dut.io.updatePC.poke("hABCD0000".U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)

      dut.io.update.poke(0.U)
      dut.io.predictedTaken.expect(1.U)  // Verify the FSM is in weakTaken state- current state 11


      //step 4: Verify the state transitions  all mispredicted so transition is like 01(initial value) -> 10-> 11 -> 00 -> 01
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      //
      dut.io.PC.poke(155.U)
      dut.io.update.poke(0.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(1.U) // 10
      ///
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      ///
      dut.io.PC.poke(155.U)
      dut.io.update.poke(0.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(1.U)  // 11
      ///
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      ////
      dut.io.PC.poke(155.U)
      dut.io.update.poke(0.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(0.U)  // 00
      ////
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      ////
      dut.io.PC.poke(155.U)
      dut.io.update.poke(0.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(0.U)  //01

      ///step5: BTB indexing check

      dut.io.updatePC.poke(128.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.updatePC.poke(132.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.updatePC.poke(136.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.updatePC.poke(140.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.updatePC.poke(144.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.updatePC.poke(148.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.updatePC.poke(156.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.update.poke(0.U)
      dut.io.PC.poke(128.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.valid.expect(1.U)
      dut.clock.step(1)
      dut.io.PC.poke(132.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.valid.expect(1.U)
      dut.clock.step(1)
      dut.io.PC.poke(136.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.valid.expect(1.U)
      dut.clock.step(1)
      dut.io.PC.poke(140.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.valid.expect(1.U)
      dut.clock.step(1)
      dut.io.PC.poke(144.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.valid.expect(1.U)
      dut.clock.step(1)
      dut.io.PC.poke(148.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.valid.expect(1.U)
      dut.clock.step(1)
      dut.io.PC.poke(156.U)
      dut.io.target.expect("hCDEF0000".U)
    }
  }
}