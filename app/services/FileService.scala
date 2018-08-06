package services

import java.io.File
import java.nio.file.{Files, Path, Paths}

import javax.inject.Inject
//import java.util.concurrent.ConcurrentHashMap

import javax.inject.Singleton
import play.api.Logger

//import scala.collection._
//import scala.collection.convert.decorateAsScala._

@Singleton
class FileService @Inject()(sparkService: SparkService) {

  private val logger = Logger(this.getClass)

  //  val map: concurrent.Map[String, File] = new ConcurrentHashMap[String, File]().asScala
  //  private var set: Set[String] = java.util.Collections.newSetFromMap(new ConcurrentHashMap[String, java.lang.Boolean]).asScala

  val baseDir = "uploaded/files/"
  val basePath = Paths.get(baseDir)

  def filePath(fName: String): String = s"uploaded/files/$fName"


  def isValid(file: File, name: String): Boolean = {
    /**
      * map by type
      * check extention
      * checkHeaders
      * checkBody
      */
    !Files.exists(Paths.get(baseDir + name))
    //    && isValidHeader()
    //    && isValidBody()
  }

  def move(file: File, fName: String): Path = {
    if (!Files.exists(basePath)) {
      Files.createDirectories(basePath)
    }
    Files.move(file.toPath, Paths.get(filePath(fName)))
    //    set += name // to be fixed (multithread issue)
    //    logger.debug(s"files: $set")
    //    path
  }

  def get(fileName: String, params: Map[String, Seq[String]]): Unit = {
    sparkService.get(filePath(fileName), params)
  }

}
