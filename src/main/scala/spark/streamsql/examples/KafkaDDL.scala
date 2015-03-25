package spark.streamsql.examples

import org.apache.spark.sql.{SQLContext, Row}
import org.apache.spark.streaming.{Duration, StreamingContext}
import spark.streamsql.StreamSQLContext

import spark.streamsql.sources.MessageToRowConverter

class MessageDelimiter extends MessageToRowConverter {
  def toRow(msg: String): Row = Row(msg.split(" "): _*)
}

object KafkaDDL {
  def main(args: Array[String]): Unit = {
    val ssc = new StreamingContext("local[10]", "test", Duration(3000))
    val sc = ssc.sparkContext

    val streamSqlContext = new StreamSQLContext(ssc, new SQLContext(sc))
    streamSqlContext.command(
      """
        |CREATE TEMPORARY TABLE t_kafka (
        |  word string
        |)
        |USING spark.streamsql.sources.KafkaSource
        |OPTIONS(
        |  zkQuorum "localhost:2181",
        |  groupId  "test",
        |  topics   "aa:1",
        |  messageToRow "spark.streamsql.examples.MessageDelimiter")
      """.stripMargin)

      streamSqlContext.sql(
      """
        |SELECT t.word, COUNT(t.word)
        |FROM (SELECT * FROM t_kafka) OVER (WINDOW '9' SECONDS, SLIDE '3' SECONDS) AS t
        |GROUP BY t.word
      """.stripMargin)
      .foreachRDD { r => r.foreach(println) }

    ssc.start()
    ssc.awaitTerminationOrTimeout(60 * 1000)
    ssc.stop()
  }

}
