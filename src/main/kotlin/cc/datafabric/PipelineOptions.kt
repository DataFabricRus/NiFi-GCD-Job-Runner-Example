package cc.datafabric

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.options.ValueProvider

interface PipelineOptions : DataflowPipelineOptions {

    fun getSource(): ValueProvider<String>

    fun setSource(source: ValueProvider<String>)

    fun getDestination(): ValueProvider<String>

    fun setDestination(source: ValueProvider<String>)

}