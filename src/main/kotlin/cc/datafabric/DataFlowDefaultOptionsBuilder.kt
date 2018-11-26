package cc.datafabric

import org.apache.beam.runners.dataflow.DataflowRunner
import org.apache.beam.sdk.options.PipelineOptionsFactory

object DataFlowDefaultOptionsBuilder {

    fun build(args: Array<String>): PipelineOptions{
        val options = PipelineOptionsFactory.fromArgs(*args).`as`(PipelineOptions::class.java)
        options.runner = DataflowRunner::class.java
        options.project =
        options.region = "europe-west1"
        options.stagingLocation =
        options.saveProfilesToGcs =
        options.maxNumWorkers = 1
        return options
    }
}