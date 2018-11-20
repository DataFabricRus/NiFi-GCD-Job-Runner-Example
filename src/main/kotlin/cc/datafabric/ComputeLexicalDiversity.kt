package cc.datafabric

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.Mean
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.KV

/**
 * Compute lexical diversity or type-token ratio (TTR): TTR = unique tokens/total number of tokens
 *
 * see https://en.wikipedia.org/wiki/Lexical_diversity
 */
fun main(args: Array<String>) {

    val options = DataFlowDefaultOptionsBuilder.build(args)
    options.jobName = "compute-lexical-diversity"
    options.templateLocation =

    val p = Pipeline.create(options)
    p
        .apply(
            TextIO
                .read()
                .from(options.getSource())
        )
        .apply(
            ParDo.of(object : DoFn<String, KV<String, Int>>() {
                @ProcessElement
                fun processElement(c: ProcessContext) {
                    val elements = c.element().split("/")
                    c.output(KV.of(elements[0], elements[2].toInt()))
                }
            }))
        .apply(Mean.perKey<String, Int>())
        .apply(
            ParDo.of(object : DoFn<KV<String, Double>, String>() {
                @ProcessElement
                fun processElement(c: ProcessContext) {
                    c.output("${c.element().key } TTR = ${(1.0).div(c.element().value)}")
                }
            }))
        .apply(
            TextIO
                .write()
                .to(options.getDestination())
                .withNumShards(1)
                .withSuffix(".txt")
        )

    p.run()
}