package cc.datafabric

import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.coders.StringUtf8Coder
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.transforms.Create
import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.Flatten
import org.apache.beam.sdk.transforms.GroupByKey
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.util.GcsUtil
import org.apache.beam.sdk.util.gcsfs.GcsPath
import org.apache.beam.sdk.values.KV
import java.nio.channels.Channels
import java.util.Scanner

val gcsFactory = GcsUtil.GcsUtilFactory()

fun main(args: Array<String>) {
    val options = DataFlowDefaultOptionsBuilder.build(args)
    options.jobName = "build-vocabulary"
    options.templateLocation =

    val p = Pipeline.create(options)
    p
        .apply(Create.ofProvider(options.getSource(), StringUtf8Coder.of()))
        .apply(
            ParDo.of(
                object : DoFn<String, List<@JvmSuppressWildcards String>>() {
                    @ProcessElement
                    fun processElement(c: ProcessContext) {
                        val out = gcsFactory
                            .create(c.pipelineOptions)
                            .expand(GcsPath.fromUri(c.element()))
                            .map { x -> x.toString() }
                        c.output(out)
                    }

                })
        )
        .apply(Flatten.iterables())
        .apply(
            ParDo
                .of(object : DoFn<String, KV<KV<String, String>, String>>() {
                    @ProcessElement
                    fun processElement(c: ProcessContext) {
                        val path = c.element()
                        val gcsUtil = gcsFactory.create(c.pipelineOptions)
                        val byteChannel = gcsUtil.open(GcsPath.fromUri(path))
                        val inputStream = Channels.newInputStream(byteChannel)
                        val scanner = Scanner(inputStream)
                        while (scanner.hasNext()) {
                            val word = scanner
                                .next()
                                .replace("[^A-Za-z]".toRegex(), "")
                                .toLowerCase()
                            c.output(KV.of(KV.of(path.substringAfterLast("/"), word), word))
                        }

                    }
                })
        )
        .apply(GroupByKey.create())
        .apply(
            ParDo.of(object : DoFn<KV<KV<String, String>, Iterable<@JvmSuppressWildcards String>>, String>() {
                @ProcessElement
                fun processElement(c: ProcessContext) {
                    val element = c.element()
                    c.output("${element.key!!.key!!}/${element.key!!.value}/${element.value.toList().size}")
                }
            }))
        .apply(
            TextIO
                .write()
                .to(options.getDestination())
                .withSuffix(".txt")
        )


    p.run()
}