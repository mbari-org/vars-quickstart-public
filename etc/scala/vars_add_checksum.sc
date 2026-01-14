#!/usr/bin/env -S scala shebang

//> using file project.scala

import java.sql.DriverManager

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.net.URL
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.MessageDigest
import java.util.HexFormat


object Sha512 {

  val hexFormat = HexFormat.of()

  /**
   * Calculate SHA-512 from an inputstream. Remember to close the inputstream yourself.
   *
   * @param inputStream The inputstream to read
   * @return
   */
  def apply(inputStream: InputStream): Array[Byte] = {
    val digest   = MessageDigest.getInstance("SHA-512")
    val buffer   = Array.ofDim[Byte](1048576) // 1MB
    var sizeRead = -1
    var ok       = true
    while (ok) {
      sizeRead = inputStream.read(buffer)
      if (sizeRead == -1) ok = false
      else digest.update(buffer, 0, sizeRead)
    }
    digest.digest()
  }

  def apply(url: URL): Array[Byte] = {
    val in  = new BufferedInputStream(url.openStream())
    val sha = apply(in)
    in.close()
    sha
  }

  def apply(file: File): Array[Byte] = {
    if (file.isDirectory) {
      throw new RuntimeException(s"$file is a directory. You can't checksum a directory.")
    }
    val in  = new BufferedInputStream(new FileInputStream(file))
    val sha = apply(in)
    in.close()
    sha
  }

  def apply(path: Path): Array[Byte] = {
    if (path.toFile.isDirectory) {
      throw new RuntimeException(s"$path is a directory. You can't checksum a directory.")
    }
    val in  = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ))
    val sha = apply(in)
    in.close()
    sha
  }

  def toHexString(sha: Array[Byte]): String = hexFormat.formatHex(sha).toLowerCase

  def fromHexString(hex: String): Array[Byte] = hexFormat.parseHex(hex)

}

def updateCheckSum(dbUrl: String, dbUser: String, dbPwd: String, url: String): Unit = {
  val connection = DriverManager.getConnection(dbUrl, dbUser, dbPwd)
  val statement = connection.createStatement()
  val sha512 = Sha512(new URL(url))
  val hex = Sha512.toHexString(sha512)
  val sql = s"""
  |UPDATE 
  |  VIDEO_REFERENCES
  |SET
  |  SHA512 = '${hex}' 
  |WHERE
  |  URI = '$url'
  |
  |""".stripMargin
  val n = statement.executeUpdate(sql)
  if (n == 1) println(s"SUCCESS! Set checksum for $url to $hex")
  else println(s"FAILURE! Could not set checksum for $url")
}


if (args.length != 1) {
  println("""Add a checksum to a media
  |
  | Usage: 
  |   AddCheckSum.sc <url>
  |
  | Environment variables:
  |   ONI_DATABASE_PASSWORD - database password
  |   ONI_DATABASE_USER     - database user
  |   M3_PUBLIC_JDBC_URL    - JDBC URL to the M3_VARS database
  |
  | Arguments:
  |  url: The url (file or http) to the medias
  |  
  |""".stripMargin)
  System.exit(1)
}

val dbPwd = System.getenv("ONI_DATABASE_PASSWORD")
val dbUrl = System.getenv("M3_PUBLIC_JDBC_URL")
val dbUser = System.getenv("ONI_DATABASE_USER")

if (dbPwd == null) {
  println("Please set an environment variable `ONI_DATABASE_PASSWORD` with the ONI database password")
}
else {
  updateCheckSum(dbUrl, dbUser, dbPwd, args(0))
}