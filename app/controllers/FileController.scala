package controllers

import java.io.File
import java.nio.file.{Files, Path}

import akka.stream.IOResult
import akka.stream.scaladsl._
import akka.util.ByteString
import javax.inject._
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.streams._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.core.parsers.Multipart.FileInfo
import services.FileService

import scala.concurrent.{ExecutionContext, Future}

case class FormData(name: String)

@Singleton
class FileController @Inject()(cc: MessagesControllerComponents, fileService: FileService)(implicit exec: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = Logger(this.getClass)

  val form = Form(
    mapping(
      "name" -> text
    )(FormData.apply)(FormData.unapply)
  )

  /**
    * Renders a file upload page.
    */
  def index = Action { implicit request =>
    Ok(views.html.index(form))
  }

  type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

  /**
    * Uses a custom FilePartHandler to return a type of "File" rather than
    * using Play's TemporaryFile class.  Deletion must happen explicitly on
    * completion, rather than TemporaryFile (which uses finalization to
    * delete temporary files).
    *
    * @return
    */
  private def handleFilePartAsFile: FilePartHandler[File] = {
    case FileInfo(partName, filename, contentType) =>
      val path: Path = Files.createTempFile("multipartBody", "tempFile")
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(path)
      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(fileSink)
      accumulator.map {
        case IOResult(count, status) =>
          logger.info(s"count = $count, status = $status")
          FilePart(partName, filename, contentType, path.toFile)
      }
  }

  /**
    * A generic operation on the temporary file that deletes the temp file after completion.
    * File has to be validated and added to storage (in our case to disk)
    */
  private def operateOnFile(file: File, name: String) = {
    if (fileService.isValid(file, name)) {
      fileService.move(file, name)
    } else {
      Files.deleteIfExists(file.toPath)
      // TODO throw message to handle situation in case file with specified name already loaded onto server
    }
  }

  /**
    * Uploads a multipart file as a POST request.
    *
    * @return
    */
  def upload = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    val fileOption = request.body.file("name").map {
      case FilePart(key, filename, contentType, file) =>
        logger.info(s"key = ${key}, filename = ${filename}, contentType = ${contentType}, file = $file")
        operateOnFile(file, filename)
        filename
    }
    Created(s"file = ${fileOption.getOrElse("no file")}")
  }

  /**
    * Returns csv file with specifying filtering and sorting
    *
    * @param fileName file to be loaded
    * @param sort     field participated in sort operation
    * @param filter   field participated in filter operation
    */
  def get(fileName: String) = Action {
  implicit request =>
    val params = request.queryString
    fileService.get(fileName, params)
    // todo loaded, filtered, and sorted csv return to UI
    Ok(s"file = $fileName, params = $params")
  }

  /**
    * Deletes file by specified fileName on server
    *
    * @param fileName - file to be deleted
    * @return
    */
  def delete(fileName: String) = Action {
    Accepted(s"file = $fileName")
  }

}
