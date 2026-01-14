#!/usr/bin/env -S scala shebang

//> using file project.scala

/*
Brian Schlining
Copyright 2022, Monterey Bay Aquarium Research Institute
*/

import java.sql.Connection
import java.sql.DriverManager
import org.jasypt.util.password.BasicPasswordEncryptor

// HACK: Not sure why this is needed now. Was working previously without it
Class.forName("org.postgresql.Driver") 

def checkUserExists(db: Connection, user: String): Boolean = {
  val stmt = db.createStatement
  val rs = stmt.executeQuery(s"select count(*) from USERACCOUNT where username = '$user'")
  rs.next
  val count = rs.getInt(1)
  rs.close
  stmt.close
  count > 0
}

def createUser(dbUrl: String, dbUser: String, dbPwd: String, username: String): Unit = {
  val connection = DriverManager.getConnection(dbUrl, dbUser, dbPwd)
  connection.setAutoCommit(false)
  val exists = checkUserExists(connection, username)
  if (!exists) {
    println(s"Creating user '$username'")
    val console = System.console()
    val firstName = console.readLine("First name: ")
    val lastName = console.readLine("Last name: ")
    val email = console.readLine("Email: ")
    val affiliation = console.readLine("Affiliation: ")
    print("Password: ")
    val pw0 = new String(console.readPassword())
    print("Password (again): ")
    val pw1 = new String(console.readPassword())

    if (pw0 != pw1) {
      println("The passwords you entered do not match.")
    }
    else {

      require(pw0.size > 0, "Password must be at least one character long")
      require(firstName.size > 1, "First name must be at least two characters long")
      require(lastName.size > 1, "Last name must be at least two characters long")
      require(email.size > 5, "Email must be at least five characters long")      
      require(affiliation.size > 1, "Affiliation must be at least two characters long")

      val encryptor = new BasicPasswordEncryptor
      val pw = encryptor.encryptPassword(pw0)
      val stmt = connection.createStatement
      val rs = stmt.executeQuery("select nextid from uniqueid u where tablename = 'UserName'")
      rs.next
      val id = rs.getInt(1)
      val nextId = id + 1
      stmt.executeUpdate(s"update uniqueid set nextid = $nextId where tablename = 'UserName'")
      stmt.executeUpdate(s"insert into USERACCOUNT (id, username, firstname, lastname, email, affiliation, password) values ($nextId, '$username', '$firstName', '$lastName', '$email', '$affiliation', '$pw')")
      connection.commit()
      stmt.close
      print("User created.")
    }
  }
  else {
    println(s"User '$username' already exists")
  }
  connection.close
}

if (args.length != 1) {
  println("""Create a new user in the VARS_KB database
    | 
    | Usage: 
    |   export VARS_PWD=<database password>
    |   CreateUser.sc <username>
    |
    | Environment variables:
    |   ONI_DATABASE_PASSWORD - database password
    |   ONI_DATABASE_USER     - database user
    |   M3_PUBLIC_JDBC_URL    - JDBC URL to the M3_VARS database
    |
    | Arguments:
    |  username: The user name to create
    |  
    |""".stripMargin('|'))
  System.exit(1)
}

val dbPwd = System.getenv("ONI_DATABASE_PASSWORD")
val dbUrl = System.getenv("M3_PUBLIC_JDBC_URL")
val dbUser = System.getenv("ONI_DATABASE_USER")
if (dbPwd == null) {
  println("Please set an environment variable `ONI_DATABASE_PASSWORD` with the ONI database password")
}
else {
  var username = args(0)
  if (username.size < 2) {
    println("Username must be at least two characters long")
  }
  else {
    createUser(dbUrl, dbUser, dbPwd, username)
  }
}
  
