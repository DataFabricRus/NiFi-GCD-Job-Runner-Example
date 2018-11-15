package cc.datafabric

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.GroupByKey
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.KV

fun main(args: Array<String>) {

    val options = DataFlowDefaultOptionsBuilder.build(args)
    options.jobName = "investigate-vocabulary-diversity"
    options.templateLocation =

    val p = Pipeline.create(options)
    p
        .apply(
            TextIO
                .read()
                .from(options.getSource())
        )
        .apply(
            ParDo.of(object : DoFn<String, KV<String, String>>() {
                @ProcessElement
                fun processElement(c: ProcessContext) {
                    val elements = c.element().split("/")

                    c.output(KV.of(elements[0], elements[1]))
                }
            }))
        .apply(GroupByKey.create())
        .apply(
            ParDo.of(object : DoFn<KV<String, Iterable<@kotlin.jvm.JvmSuppressWildcards String>>, String>() {
                @ProcessElement
                fun processElement(c: ProcessContext) {
                    val element = c.element()
                    c.output("""${element.key} - ${element.value.toList().size}""")
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