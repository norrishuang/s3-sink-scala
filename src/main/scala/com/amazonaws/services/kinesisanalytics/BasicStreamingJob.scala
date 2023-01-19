package com.amazonaws.services.kinesisanalytics

import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.apache.flink.api.common.serialization.{SimpleStringEncoder, SimpleStringSchema}
import org.apache.flink.api.common.typeinfo.Types
import org.apache.flink.api.java.tuple.Tuple2
import org.apache.flink.connector.kinesis.sink.KinesisStreamsSink
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants

import java.util
import java.util.{Map, Properties}

private val streamNameKey = "stream.name"
private val defaultInputStreamName = "ExampleInputStream"
private val region = "us-east-1"

def createSource: FlinkKinesisConsumer[String] = {
  val applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties
//  val inputProperties = applicationProperties.get("ConsumerConfigProperties")
  val inputProperties = new Properties
  inputProperties.setProperty("aws.region", region)
  inputProperties.setProperty(ConsumerConfigConstants.STREAM_INITIAL_POSITION, "LATEST")
  new FlinkKinesisConsumer[String](defaultInputStreamName,
    new SimpleStringSchema, inputProperties)
}

def createSink: StreamingFileSink[String] = {
  val applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties
//  val s3SinkPath = applicationProperties.get("ProducerConfigProperties").getProperty("s3.sink.path")
 val s3SinkPath = "s3://myemr-bucket-01/data/mydata/kda/data01/"

  StreamingFileSink
    .forRowFormat(new Path(s3SinkPath), new SimpleStringEncoder[String]("UTF-8"))
    .build()
}

@main def main(): Unit = {
//  val environment = StreamExecutionEnvironment.getExecutionEnvironment
  val environment = StreamExecutionEnvironment.createLocalEnvironment
  val jsonParser = new ObjectMapper()

  environment.enableCheckpointing(30000)

  environment.addSource(createSource)
    .map { value =>
      val jsonNode = jsonParser.readValue(value, classOf[JsonNode])
      System.out.println(value)
      new Tuple2[String, Double](jsonNode.get("ticker").toString, jsonNode.get("price").asDouble)
    }
    .returns(Types.TUPLE(Types.STRING, Types.DOUBLE))
    .keyBy(v => v.f0) // Logically partition the stream for each word
    .window(SlidingProcessingTimeWindows.of(Time.seconds(10), Time.seconds(5)))
    .min(1) // Calculate minimum price per ticker over the window
    .map { value => value.f0 + String.format(",%.2f", value.f1) + "\n" }
    .addSink(createSink)
  environment.execute("Flink Streaming Scala Example")
}