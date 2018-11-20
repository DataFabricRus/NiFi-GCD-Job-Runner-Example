# NiFi-GCD-Job-Runner-Example
An example demonstrates NiFi Google Cloud (GC) Dataflow Job runner
## Overview
The repository contains an example to demonstrate [NiFi GC Dataflow Job Runner](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner) functioning. The example includes two GC Dataflow jobs that are assumed to be run in a sequence. This [README](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/README.md) describes in details only the part relevant for the repository itself. The overall example description is described on [TBD].

The example suggests performing four steps:
1. Integrate NiFi GC Dataflow Job Runner packet into the Apache NiFi bundle
1. Create job templates based on the code from this repository
1. Create and tune Apache NiFi data flow based on the template from this repository
1. Run the data flow

## Integration of NiFi GC Dataflow Job Runner packet into the Apache NiFi bundle
Instruction on how to do this can be found here [NiFi GC Dataflow Job Runner](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner)

## Creation of job templates
### General Information
[GC Dataflow](https://cloud.google.com/dataflow/) jobs are implemented on [Apache Beam](https://beam.apache.org/) framework in a form of pipelines. Apache Beam allows running a pipeline for data processing on different cluster platforms. To make this possible Apache Beam provides a number of platform-dependent [runners](https://beam.apache.org/documentation/runners/capability-matrix/). One of these platforms is Google Cloud Platform (GCP) and the corresponding runner is a [DataflowRunner](https://beam.apache.org/documentation/runners/dataflow/). The provided example is based on GCP.

After a code of a job processing some data is written one has three options: 1) to run it locally with [DirectRunner](https://beam.apache.org/documentation/runners/direct/) (used for tests only, not suitable for large data processing); 2) to run it directly with GC Dataflow; 3) to create a job template and run it lately. The third option is used in the example.

### Introduction
The example provides two Apache Beam pipelines. The first pipeline is [BuildVocabulary](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/BuildVocabulary.kt). It accepts texts stored in [GC Storage] (https://cloud.google.com/storage/) and build a vocabulary of word stems. For stems extraction [java version](https://tartarus.org/martin/PorterStemmer/java.txt) of [Porter Stemming Algorithm](https://tartarus.org/martin/PorterStemmer/) is used. The vocabulary is a text document with the following structure:

```<a text file name>/<a unique word stem>/<a number of the word stems in the text file>```

The second pipeline is [ComputeLexicalDiversity](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/ComputeLexicalDiversity.kt). It accepts vocabulary from the previous pipeline and computes [lexical diversity](https://en.wikipedia.org/wiki/Lexical_diversity) (or Type-token ratio, TTR) of some text using the formula:

```TTR = <a number unique word stems> / <a total number of words>```

### Job templating
1. Clone project as usual. The project is not runnable from the a scratch because some specific properties must be set first
1. So, make it runnable again:
   1. Go to [DataFlowDefaultOptionsBuilder](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/DataFlowDefaultOptionsBuilder.kt) and set parameters of the runner. You may consult with [the official documentation](https://cloud.google.com/dataflow/pipelines/specifying-exec-params). Only ```options.region``` is set to "europe-west1" and ```options.maxNumWorkers``` is set to one. Change them on more appropriate ones if you want.
   1. Go to [BuildVocabulary](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/BuildVocabulary.kt) and set ```options.templateLocation```. It will be a template path of the job building vocabulary. It should look like ```gs://bucket name/some path/template name```, i.e. it is a path within GC Storage
   1. Do the same thing for [ComputeLexicalDiversity](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/ComputeLexicalDiversity.kt) 
   
   *Pay attention that to create and push a template it is necessary: 1) set ```options.runner``` to DataflowRunner; 2) set ```options.templateLocation``` to template path. If the option is not set and you will run the code then the job itself will be launched on GC Dataflow platform*
1. Templates can be now created and push to GC Storage
   1. Run main of [BuildVocabulary](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/BuildVocabulary.kt)
   1. Run main of [ComputeLexicalDiversity](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/ComputeLexicalDiversity.kt)
1. As a result you should see templates on the given paths

## Creation & Tuning of Apache NiFi data flow
Created job templates are supposed to be used in an illustrative Apache NiFi data flow. NiFi data flow specification is fixed in the [XML file](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/test/resources/GC_Dataflow_Job_Runner_Example_Template.xml). The flow uses special processors called [NiFi GC Dataflow Job Runners](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner). It is assumed that NiFi platform is already extended with them (see [NiFi GC Dataflow Job Runner](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner))

The flow specifies the following processing: 
1. List text file names stored in GC Storage on the given path
1. For each text file a stem vocabulary is builded with [BuildVocabulary](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/BuildVocabulary.kt) job
1. For each vocabulary a TTR is computed with [ComputeLexicalDiversity](https://github.com/DataFabricRus/NiFi-GCD-Job-Runner-Example/blob/master/src/main/kotlin/cc/datafabric/ComputeLexicalDiversity.kt) job
While calling GC Dataflow jobs special notifications of job statuses are generated. They are passed to the specified Slack channel
