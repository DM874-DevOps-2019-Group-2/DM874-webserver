package services

import java.io.InputStream

import scala.concurrent.{ExecutionContext, Future}

class FileStore()(implicit ec: ExecutionContext) {
  def put(bucket: String, key: String, data: String) = {
    ???
  }

  def put(bucket: String, key: String, data: InputStream) = {
    ???
  }

  def putAsync(bucket: String, key: String, data: String) = Future{ put(bucket, key, data) }

  def putAsync(bucket: String, key: String, data: InputStream) = Future{ put(bucket, key, data) }

  def get(bucket: String, key: String): String = {
    ???
  }

  def getAsync(bucket: String, key: String): Future[String] = Future{ get(bucket, key) }
}
