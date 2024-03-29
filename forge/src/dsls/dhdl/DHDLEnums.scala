package ppl.dsl.forge
package dsls
package dhdl

trait DHDLEnums {
  this: DHDLDSL =>

  def importDHDLEnums () = {
    /* Reg Type Enum */
    val RegType = lookupTpe("RegType", stage=compile)
    identifier (RegType) ("ArgumentIn")
    identifier (RegType) ("ArgumentOut")
    identifier (RegType) ("Regular")

    /* Controller style enum */
    val ControlType = lookupTpe("ControlType", stage=compile)
    identifier (ControlType) ("InnerPipe")
    identifier (ControlType) ("StreamPipe")
    identifier (ControlType) ("CoarsePipe")
    identifier (ControlType) ("SequentialPipe")
    identifier (ControlType) ("ForkJoin")
  }

}
