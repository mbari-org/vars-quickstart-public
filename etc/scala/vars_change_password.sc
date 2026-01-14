#!/usr/bin/env -S scala shebang

//> using file project.scala

/*
Brian Schlining
Copyright 2022, Monterey Bay Aquarium Research Institute
*/

import java.sql.DriverManager
import org.jasypt.util.password.BasicPasswordEncryptor


def changePassword(dbUrl: String, dbUser: String, dbPwd: String, username: String): Unit = {
  println(s"Changing password for $username")
  val console = System.console()
  print("Enter the new password:")
  val pw0 = new String(console.readPassword())
  print("Enter the new password again:")
  val pw1 = new String(console.readPassword())
  if (pw0 != pw1) {
    println("The passwords you entered do not match.")
  }
  else {
    val encryptor = new BasicPasswordEncryptor()
    val encryptedPwd = encryptor.encryptPassword(pw0)
    val connection = DriverManager.getConnection(dbUrl, dbUser, dbPwd)
    val statement = connection.createStatement()
    val sql = s"""
      |UPDATE
      |  UserAccount
      |SET
      |  Password = '$encryptedPwd'
      |WHERE
      |  UserName = '${username}'
      """.stripMargin('|')
    val n = statement.executeUpdate(sql)
    if (n == 1) println("The password was succesfully changed")
    else println("Failed to change password")
  }

}

if (args.length != 1) {
  println("""Change a users password in the VARS_KB database
    | 
    | Usage: 
    |   ChangePassword.sc <username>
    |
    | Environment variables:
    |   ONI_DATABASE_PASSWORD - database password
    |   ONI_DATABASE_USER     - database user
    |   M3_PUBLIC_JDBC_URL    - JDBC URL to the M3_VARS database
    |
    | Arguments:
    |  username: The user name to change the password for
    |  
    |""".stripMargin('|'))
    System.exit(1)
}

val dbPwd = System.getenv("ONI_DATABASE_PASSWORD")
val dbUser = System.getenv("ONI_DATABASE_USER")
val dbUrl = System.getenv("M3_PUBLIC_JDBC_URL")
if (dbPwd == null) {
  println("Please set an environment variable `ONI_DATABASE_PASSWORD` with the ONI database password")
}
else {
  changePassword(dbUrl, dbUser, dbPwd, args(0))
}


