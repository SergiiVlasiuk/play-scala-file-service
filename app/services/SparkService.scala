package services

import javax.inject.Singleton
import org.apache.hadoop.fs.LocalFileSystem
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

@Singleton
class SparkService {


  def getValidFields(fieldNames: Option[Seq[String]], existing: Array[String]): List[String] = {
    val strings = fieldNames
      .map(_.mkString)
      .map(_.split(','))
        .getOrElse(Array())
    strings.filter(s => existing.contains(s))
        .toList
  }

  def get(str: String, params: Map[String, Seq[String]]): Unit = {
    val conf = new SparkConf().set("spark.executor.memory", "4g")
    conf.set("fs.file.impl", classOf[LocalFileSystem].getName)
    val session = SparkSession.builder()
      .appName("play_scala_file_service")
      .master("local[*]")
      .config(conf)
      .getOrCreate()
    val dataFrameReader = session.read
    val responses = dataFrameReader
      .option("header", "true")
      .option("inferSchema", value = true)
      //      .csv()
      .csv(str)

    responses.columns
    val sortValues: List[String] = getValidFields(params.get("sort"), responses.columns)

  }

}
