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

      // Test Case 1: Write to the BTB and check valid entry
      dut.io.PC.poke("hABCD0000".U)
      dut.io.update.poke(0.U)  // No update initially
      dut.io.updatePC.poke(0.U)
      dut.io.updateTarget.poke(0.U)
      dut.io.mispredicted.poke(0.U)

      dut.clock.step(1)
      dut.io.valid.expect(0.U)  // No valid entry yet

      // Write the first instruction
      dut.io.update.poke(1.U)  // Now allow updates to the BTB
      dut.io.updatePC.poke("hABCD0000".U)  // Writing for PC "hABCD0000"
      dut.io.updateTarget.poke(10.U)  // Target address 10
      dut.io.mispredicted.poke(1.U)  // Simulate a misprediction

      dut.clock.step(1)

      // Checking if the entry is valid and the target is correctly updated
      dut.io.PC.poke("hABCD0000".U)
      dut.clock.step(1)
      dut.io.valid.expect(1.U)  // BTB entry should now be valid
      dut.io.target.expect(10.U)  // Target should be 10
      dut.io.predictedTaken.expect(0.U)  // initial value is 01 - predict- taken is zero

      //----------- Test Case 2: Reading and writing at the same PC address
      dut.io.PC.poke("hABCF0000".U)
      dut.io.update.poke(1.U)  // Writing to the BTB at the same PC
      dut.io.updatePC.poke("hABCF0000".U)
      dut.io.updateTarget.poke(30.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)

      // Ensure correct valid and target
      dut.io.valid.expect(1.U)
      dut.io.target.expect(30.U)
      dut.io.predictedTaken.expect(1.U)

      dut.clock.step(1)
      dut.io.PC.poke("hABCD0000".U)
      dut.clock.step(1)
      dut.io.PC.poke("hABCF0000".U)
      dut.io.valid.expect(1.U)
      dut.io.target.expect(30.U)
      dut.io.predictedTaken.expect(0.U)

      // Test Case 3: Checking Replacement Logic in BTB
      dut.io.update.poke(1.U)  // Write a new entry at the same set
      dut.io.updatePC.poke("hABFF0000".U)
      dut.io.updateTarget.poke(40.U)
      dut.io.mispredicted.poke(0.U)

      dut.clock.step(1)
      dut.io.PC.poke("hABCD0000".U)  // This entry should be evicted
      dut.clock.step(1)
      dut.io.valid.expect(0.U)  // Entry for "hABCD0000" should no longer be valid

      dut.clock.step(1)
      dut.io.PC.poke("hABCF0000".U)
      dut.clock.step(1)
      dut.io.valid.expect(1.U)
      dut.io.target.expect(30.U)
      dut.io.predictedTaken.expect(0.U)

      dut.clock.step(1)
      dut.io.PC.poke("hABFF0000".U)
      dut.clock.step(1)
      dut.io.valid.expect(1.U)
      dut.io.target.expect(40.U)
      dut.io.predictedTaken.expect(0.U)

      // Step 4: FSM Test - State Transitions

      // Test FSM transition from strongNotTaken → weakNotTaken → strongTaken
      dut.io.PC.poke("hABCD0000".U)
      dut.io.update.poke(1.U)
      dut.io.updatePC.poke("hABCD0000".U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.mispredicted.poke(0.U)
      dut.clock.step(1)
      dut.io.valid.expect(1.U)
      dut.io.target.expect("hCDEF0000".U)
      dut.io.predictedTaken.expect(0.U)  // strongNotTaken initially

      // Step 1: First misprediction should move to weakNotTaken
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(1.U)  // weakNotTaken (no change to taken)

      // Step 2: Second misprediction should move to strongTaken
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(1.U)  // strongTaken after second misprediction

      // Step 3: Verify final state after all transitions
      dut.io.PC.poke("hABCD0000".U)
      dut.io.update.poke(1.U)
      dut.io.updatePC.poke("hABCD0000".U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(0.U)  // Verify the FSM is in strongTaken state


      //step 4: Verify the state transitions
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      //
      dut.io.PC.poke(155.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(0.U)
      ///
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      ///
      dut.io.PC.poke(155.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(1.U)
      ///
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      ////
      dut.io.PC.poke(155.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(0.U)
      ////
      dut.io.updatePC.poke(155.U)
      dut.io.updateTarget.poke("hCDEF0000".U)
      dut.io.update.poke(1.U)
      dut.io.mispredicted.poke(1.U)
      dut.clock.step(1)
      ////
      dut.io.PC.poke(155.U)
      dut.clock.step(1)
      dut.io.predictedTaken.expect(1.U)

    }
  }
}